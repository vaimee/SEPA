package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.BindException;
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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityManager;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.timing.Timings;

public class WebsocketServer extends WebSocketServer implements WebsocketServerMBean {
	private static final Logger logger = LogManager.getLogger();

	protected Scheduler scheduler;

	protected String getWelcomeMessage() {
		return "SPARQL 1.1 Subscribe | ws://%s:%d%s";
	}

	protected String welcomeMessage;

	private String path;

	// Fragmentation support
	private HashMap<WebSocket, String> fragmentedMessages = new HashMap<WebSocket, String>();

	// JMX
	protected WebsocketBeans jmx = new WebsocketBeans();

	// Active sockets
	private HashMap<WebSocket, WebsocketEventHandler> activeSockets = new HashMap<WebSocket, WebsocketEventHandler>();

	// Dependability manager
	private DependabilityManager dependabilityMng;

	public WebsocketServer(int port, String path, Scheduler scheduler, DependabilityManager dependabilityMng)
			throws SEPAProtocolException {
		super(new InetSocketAddress(port));

		if (path == null || scheduler == null || dependabilityMng == null)
			throw new SEPAProtocolException(new IllegalArgumentException("One or more arguments are null"));

		this.scheduler = scheduler;
		this.dependabilityMng = dependabilityMng;
		this.path = path;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		String address = getAddress().getAddress().toString();

		try {
			address = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error(e.getMessage());
			throw new SEPAProtocolException(e);
		}

		welcomeMessage = String.format(getWelcomeMessage(), address, port, path);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.debug("@onOpen: " + conn + " Resource descriptor: " + conn.getResourceDescriptor());

		if (!conn.getResourceDescriptor().equals(path)) {
			logger.warn("Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST,
					"Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			conn.send(response.toString());
			return;
		}

		fragmentedMessages.put(conn, null);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		logger.debug("@onClose: " + conn + " Reason: " + reason + " Code: " + code + " Remote: " + remote);

		if (!conn.getResourceDescriptor().equals(path))
			return;

		fragmentedMessages.remove(conn);

		// Unsubscribe all SPUs
		dependabilityMng.onBrokenSocket(conn);

		// Remove active socket
		activeSockets.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		jmx.onMessage();

		logger.debug("Message from: " + conn.getRemoteSocketAddress() + " [" + message + "]");

		if (!conn.getResourceDescriptor().equals(path)) {
			logger.warn("Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST,
					"Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			conn.send(response.toString());
			return;
		}

		// Parse the request
		Request req = parseRequest(message, conn);
		if (req == null) {
			logger.error("Failed to parse message: "+req);
			return;
		}
		
		// Add active socket
		if (!activeSockets.containsKey(conn)) {
			activeSockets.put(conn, new WebsocketEventHandler(conn, jmx, dependabilityMng));
		}
		activeSockets.get(conn).startTiming();

		Timings.log(req);

		// Schedule the request
		scheduler.schedule(req, activeSockets.get(conn));
	}

	/**
	 * SPARQL 1.1 Subscribe language
	 * 
	 * <pre>
	{"subscribe":{
		"sparql":"SPARQL Query 1.1", 
		"authorization": "Bearer JWT", (optional)
		"alias":"an alias for the subscription", (optional)
		"default-graph-uri": "graphURI", (optional)
		"named-graph-uri": "graphURI" (optional)
	}}
	
	{"unsubscribe":{
		"spuid":"SPUID", 
		"authorization": "Bearer JWT" (optional)
	}}
	 * </pre>
	 */
	protected Request parseRequest(String request, WebSocket conn)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;
		ErrorResponse error;

		try {
			req = new JsonParser().parse(request).getAsJsonObject();
		} catch (JsonParseException e) {
			error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "JsonParseException: " + request);
			conn.send(error.toString());
			return null;
		}

		if (req.has("subscribe")) {
			String sparql = null;
			String alias = null;
			String defaultGraphUri = null;
			String namedGraphUri = null;

			try {
				sparql = req.get("subscribe").getAsJsonObject().get("sparql").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "sparql member not found: " + request);
				conn.send(error.toString());
				return null;
			}

			try {
				alias = req.get("subscribe").getAsJsonObject().get("alias").getAsString();
			} catch (Exception e) {
			}

			try {
				defaultGraphUri = req.get("subscribe").getAsJsonObject().get("default-graph-uri").getAsString();
			} catch (Exception e) {
			}

			try {
				namedGraphUri = req.get("subscribe").getAsJsonObject().get("named-graph-uri").getAsString();
			} catch (Exception e) {
			}

			return new SubscribeRequest(sparql, alias, defaultGraphUri, namedGraphUri, null);
		} else if (req.has("unsubscribe")) {
			String spuid;
			try {
				spuid = req.get("unsubscribe").getAsJsonObject().get("spuid").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "spuid member not found: " + request);
				conn.send(error.toString());
				return null;
			}
			
			return new UnsubscribeRequest(spuid);
		}
		
		error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Bad request: " + request);
		conn.send(error.toString());
		return null;
	}

	/**
	 * Example: for a text message sent as three fragments, the first fragment would
	 * have an opcode of 0x1 and a FIN bit clear, the second fragment would have an
	 * opcode of 0x0 and a FIN bit clear, and the third fragment would have an
	 * opcode of 0x0 and a FIN bit that is set.
	 */

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		logger.debug("@onFragment WebSocket: <" + conn + "> Fragment data:<" + fragment + ">");

		if (!conn.getResourceDescriptor().equals(path))
			return;

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
		logger.error("@onError: " + conn + " Exception: " + ex);

		if (ex.getClass().equals(BindException.class)) {
			logger.fatal("Failed to start. Exit");
			System.exit(-1);
		}

		if (!conn.getResourceDescriptor().equals(path))
			return;

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
