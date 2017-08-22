package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.protocol.websocket.handler.SPARQL11SubscribeHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketServer extends WebSocketServer implements WebsocketServerMBean {
	private Logger logger = LogManager.getLogger("WebsocketServer");

	protected Scheduler scheduler;

	protected ConcurrentHashMap<WebSocket, SPARQL11SubscribeHandler> activeSubscriptions = new ConcurrentHashMap<WebSocket, SPARQL11SubscribeHandler>();

	protected String getWelcomeMessage() {
		return "Subscribe            | ws://%s:%d%s";
	}

	protected String welcomeMessage;

	// Fragmentation support
	private HashMap<WebSocket, String> fragmentedMessages = new HashMap<WebSocket, String>();

	//JMX
	protected WebsocketBeans jmx = new WebsocketBeans();
	
	public WebsocketServer(int port, String path, Scheduler scheduler, int keepAlivePeriod)
			throws IllegalArgumentException, UnknownHostException {
		super(new InetSocketAddress(port));

		if (path == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;
		
		SEPABeans.registerMBean("SEPA:type="+this.getClass().getSimpleName(), this);
		
		jmx.setKeepAlive(keepAlivePeriod);
		
		welcomeMessage = String.format(getWelcomeMessage(), getAddress().getAddress(), port, path);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.debug("@onConnect");

		fragmentedMessages.put(conn, null);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		logger.debug("@onClose " + reason + " (" + code + ") remote:" + remote);

		fragmentedMessages.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		jmx.onMessage();
		
		if (!activeSubscriptions.contains(conn)) {
			SPARQL11SubscribeHandler handler = new SPARQL11SubscribeHandler(scheduler, conn, jmx.getKeepAlive());
			handler.start();
			activeSubscriptions.put(conn, handler);
		}
		activeSubscriptions.get(conn).processRequest(message);
	}

	/**
	 * Example: for a text message sent as three fragments, the first
	 * fragment would have an opcode of 0x1 and a FIN bit clear, the second
	 * fragment would have an opcode of 0x0 and a FIN bit clear, and the
	 * third fragment would have an opcode of 0x0 and a FIN bit that is set.
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
	public String getRequests() {
		return jmx.getRequests(false);
	}

	@Override
	public void setKeepAlive(long period) {
		jmx.setKeepAlive(period);	
	}

	@Override
	public long getKeepAlive() {
		return jmx.getKeepAlive();
	}
}
