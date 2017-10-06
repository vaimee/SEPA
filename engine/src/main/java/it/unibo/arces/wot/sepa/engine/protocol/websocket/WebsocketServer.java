package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
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
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketServer extends WebSocketServer implements WebsocketServerMBean {
	private static final Logger logger = LogManager.getLogger("WebsocketServer");

	protected Scheduler scheduler;

	protected String getWelcomeMessage() {
		return "Subscribe            | ws://%s:%d%s";
	}

	protected String welcomeMessage;

	// Fragmentation support
	private HashMap<WebSocket, String> fragmentedMessages = new HashMap<WebSocket, String>();

	private HashMap<WebSocket, WebsocketEventHandler> eventHandlers = new HashMap<WebSocket, WebsocketEventHandler>();

	// JMX
	protected WebsocketBeans jmx = new WebsocketBeans();

	public WebsocketServer(int port, String path, Scheduler scheduler, int keepAlivePeriod)
			throws IllegalArgumentException, UnknownHostException {
		super(new InetSocketAddress(port));

		if (path == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		String address = getAddress().getAddress().toString();
		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error(e.getMessage());
		}
		welcomeMessage = String.format(getWelcomeMessage(), address, port, path);
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

		eventHandlers.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		jmx.onMessage();

		Request req = parseRequest(message);

		if (req == null) {
			logger.debug("Failed to parse: " + message);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Failed to parse: " + message);
			conn.send(response.toString());
			return;
		}

		if (eventHandlers.get(conn) == null) eventHandlers.put(conn, new WebsocketEventHandler(conn));
		 
		int requestToken = scheduler.schedule(req, eventHandlers.get(conn));

		if (requestToken == -1) {
			logger.debug("No more tokens");
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_INSUFFICIENT_STORAGE, "No more tokens");
			conn.send(response.toString());
			return;
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
	public long getMessages() {
		return jmx.getMessages();
	}

	@Override
	public long getFragmented() {
		return jmx.getFragmented();
	}

	@Override
	public long getErrors() {
		return jmx.getErrors();
	}
}
