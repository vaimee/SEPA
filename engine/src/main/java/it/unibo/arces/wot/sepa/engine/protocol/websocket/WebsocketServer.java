package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import it.unibo.arces.wot.sepa.engine.protocol.handler.SubscribeHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketServer extends WebSocketServer {
	private Logger logger = LogManager.getLogger("SubscribeServer");

	protected Scheduler scheduler;
	protected KeepAlive ping = null;

	protected HashMap<WebSocket, ResponseAndNotificationListener> activeSockets = new HashMap<WebSocket, ResponseAndNotificationListener>();
	protected HashMap<WebSocket, HashSet<String>> activeSubscriptions = new HashMap<WebSocket, HashSet<String>>();

	protected String scheme = "ws://";

	// Fragmentation support
	private HashMap<WebSocket, String> fragmentedMessages = new HashMap<WebSocket, String>();

	public WebsocketServer(int port, String path, int keepAlive, Scheduler scheduler)
			throws IllegalArgumentException, UnknownHostException {
		super(new InetSocketAddress(port));

		if (path == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;

		if (keepAlive <= 0)
			keepAlive = 5000;

		ping = new KeepAlive(keepAlive, activeSockets, activeSubscriptions, scheduler);
		ping.start();

		System.out.println("Subscribe on: " + scheme + InetAddress.getLocalHost().getHostAddress() + ":" + port + path);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.debug("@onConnect");

		synchronized (activeSockets) {
			activeSubscriptions.put(conn, new HashSet<String>());
			activeSockets.put(conn, new ResponseListener(conn, scheduler, activeSubscriptions.get(conn)));
		}
		fragmentedMessages.put(conn, null);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		logger.debug("@onClose");
		
		fragmentedMessages.remove(conn);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		new SubscribeHandler(scheduler, conn, message, activeSockets).start();
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		/*
		 * EXAMPLE: For a text message sent as three fragments, the first
		 * fragment would have an opcode of 0x1 and a FIN bit clear, the second
		 * fragment would have an opcode of 0x0 and a FIN bit clear, and the
		 * third fragment would have an opcode of 0x0 and a FIN bit that is set.
		 */
		logger.debug("@onFragment " + fragment);
		
		if (fragmentedMessages.get(conn) == null) fragmentedMessages.put(conn, new String(fragment.getPayloadData().array(),Charset.forName("UTF-8")));
		else fragmentedMessages.put(conn, fragmentedMessages.get(conn) + new String(fragment.getPayloadData().array(),Charset.forName("UTF-8")));
		
		logger.debug("Fragmented message: " + fragmentedMessages.get(conn));
		
		if (fragment.isFin()) {
			onMessage(conn,fragmentedMessages.get(conn));
			fragmentedMessages.put(conn,null);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub

	}
}
