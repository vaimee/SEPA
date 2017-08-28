package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketServer extends WebSocketServer implements WebsocketServerMBean, EventHandler {
	private static final Logger logger = LogManager.getLogger("WebsocketServer");

	protected Scheduler scheduler;

	protected HashMap<String, WebSocket> activeSubscriptions = new HashMap<String, WebSocket>();
	protected HashMap<Integer, WebSocket> scheduledRequests = new HashMap<Integer, WebSocket>();
	protected HashMap<WebSocket, HashSet<String>> activeSockets = new HashMap<WebSocket, HashSet<String>>();

	protected String getWelcomeMessage() {
		return "Subscribe            | ws://%s:%d%s";
	}

	protected String welcomeMessage;

	// Fragmentation support
	private HashMap<WebSocket, String> fragmentedMessages = new HashMap<WebSocket, String>();

	// JMX
	protected WebsocketBeans jmx = new WebsocketBeans();

	private Keepalive ping = null;

	class Keepalive extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(EngineBeans.getKeepalive());
				} catch (InterruptedException e) {
					return;
				}

				synchronized (activeSockets) {
					for (WebSocket socket : activeSockets.keySet()) {
						Ping ping = new Ping();
						try {
							socket.send(ping.toString());
						} catch (WebsocketNotConnectedException e) {
							logger.warn("Ping on closed socket");
						}
					}
				}
			}
		}
	}

	public WebsocketServer(int port, String path, Scheduler scheduler, int keepAlivePeriod, long timeout)
			throws IllegalArgumentException, UnknownHostException {
		super(new InetSocketAddress(port));

		if (path == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		welcomeMessage = String.format(getWelcomeMessage(), getAddress().getAddress(), port, path);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.debug("@onConnect");

		fragmentedMessages.put(conn, null);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		logger.debug("@onClose Reason: <" + reason + "> Code: <" + code + "> Remote: <" + remote + ">¯");

		fragmentedMessages.remove(conn);

		// Close subscription
		synchronized (activeSockets) {
			if (activeSockets.get(conn)==null) return;
			
			for (String spuid : activeSockets.get(conn)) {
				// Remove websocket related data
				activeSubscriptions.remove(spuid);
				activeSockets.get(conn).remove(spuid);
				
				// Send unsubscribe request to the scheduler
				scheduler.schedule(new UnsubscribeRequest(spuid), 0, null);
			}
			activeSockets.remove(conn);
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		jmx.onMessage();

		Request req = parseRequest(message);

		if (req == null) {
			logger.debug("Not supported request: " + message);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Not supported request: " + message);
			conn.send(response.toString());
			return;
		}

		// Schedule a new request
		int requestToken;
		requestToken = scheduler.schedule(req, EngineBeans.getTimeout(), this);

		if (requestToken == -1) {
			logger.debug("No more tokens");
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_INSUFFICIENT_STORAGE, "No more tokens");
			conn.send(response.toString());
			return;
		}
		synchronized (scheduledRequests) {
			scheduledRequests.put(requestToken, conn);
		}
	}

	/*
	 * SPARQL 1.1 Subscribe language
	 * 
	 * {"subscribe":"SPARQL Query 1.1", "authorization": "Bearer JWT",
	 * "alias":"an alias for the subscription"}
	 * 
	 * {"unsubscribe":"SPUID", "authorization": "Bearer JWT"}
	 * 
	 * If security is not required (i.e., ws), authorization key MAY be missing
	 */
	private Request parseRequest(String request)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;

		req = new JsonParser().parse(request).getAsJsonObject();

		if (req.get("subscribe") != null) {
			String sparql = req.get("subscribe").getAsString();
			if (req.get("alias") != null) {
				String alias = req.get("alias").getAsString();
				return new SubscribeRequest(sparql, alias);
			}
			return new SubscribeRequest(sparql);
		}
		if (req.get("unsubscribe") != null) {
			String spuid = req.get("unsubscribe").getAsString();
			return new UnsubscribeRequest(spuid);
		}

		return null;
	}

	@Override
	public void notifyEvent(Notification notify) {
		if (!activeSubscriptions.isEmpty())
			activeSubscriptions.get(notify.getSPUID()).send(notify.toString());
	}

	@Override
	public void notifyResponse(Response response) {
		int token = response.getToken();

		if (!scheduledRequests.containsKey(token))
			return;

		if (response.getClass().equals(SubscribeResponse.class)) {
			logger.debug("<< SUBSCRIBE response #" + token);

			WebSocket socket = scheduledRequests.get(token);
			String spuid = ((SubscribeResponse) response).getSpuid();

			synchronized (activeSockets) {
				// Register SPU ID
				activeSubscriptions.put(spuid, socket);

				// Add to active sockets
				if (activeSockets.get(socket) == null) {
					activeSockets.put(socket, new HashSet<String>());
				}
				activeSockets.get(socket).add(spuid);
			}

			if (ping == null) {
				ping = new Keepalive();
				ping.setName("Keepalive");
				ping.start();
			}
		} else if (response.getClass().equals(UnsubscribeResponse.class)) {
			logger.debug("<< UNSUBSCRIBE response #" + token + " ");
			String spuid = ((UnsubscribeResponse) response).getSpuid();

			synchronized (activeSockets) {
				WebSocket socket = activeSubscriptions.get(spuid);
				
				activeSockets.get(socket).remove(spuid);
				if (activeSockets.get(socket).isEmpty())
					activeSockets.remove(socket);

				activeSubscriptions.remove(spuid);
			}
		}

		// Send response to client
		synchronized (scheduledRequests) {
			scheduledRequests.get(token).send(response.toString());
			scheduledRequests.remove(token);
		}
	}

	/**
	 * Example: for a text message sent as three fragments, the first fragment
	 * would have an opcode of 0x1 and a FIN bit clear, the second fragment
	 * would have an opcode of 0x0 and a FIN bit clear, and the third fragment
	 * would have an opcode of 0x0 and a FIN bit that is set.
	 */

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		logger.debug("@onFragment " + fragment);

		if (fragmentedMessages.get(conn) == null)
			fragmentedMessages.put(conn, new String(fragment.getPayloadData().array(), Charset.forName("UTF-8")));
		else
			fragmentedMessages.put(conn, fragmentedMessages.get(conn)
					+ new String(fragment.getPayloadData().array(), Charset.forName("UTF-8")));

		logger.debug("Fragmented message: " + fragmentedMessages.get(conn));

		if (fragment.isFin()) {
			jmx.onFragmentedMessage();

			onMessage(conn, fragmentedMessages.get(conn));
			fragmentedMessages.put(conn, null);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		logger.error(ex);

		jmx.onError();
	}

	@Override
	public void onStart() {
		System.out.println(welcomeMessage);

		synchronized (this) {
			notify();
		}
	}

	@Override
	public void reset() {
		jmx.reset();
	}

	@Override
	public long getMessages(){
		return jmx.getMessages();
	}
	@Override
	public long getFragmented(){
		return jmx.getFragmented();
	}
	@Override
	public long getErrors(){
		return jmx.getErrors();
	}
}
