package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.protocol.handler.SubscribeHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SubscribeServer extends WebSocketServer {
//	private final HttpServer server;
	private Logger logger = LogManager.getLogger("SubscribeServer");
	
	protected Scheduler scheduler;
	private KeepAlive ping = null;
	protected HashMap<WebSocket, WebsocketListener> activeSockets = new HashMap<WebSocket, WebsocketListener>();
	
	public SubscribeServer(EngineProperties properties, Scheduler scheduler)
			throws IllegalArgumentException, UnknownHostException {
		super( new InetSocketAddress( properties.getWsPort() ) );
		
		if (properties == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");
		
		this.scheduler = scheduler;
		
		if (properties.getKeepAlivePeriod() > 0) {
			ping = new KeepAlive(properties.getKeepAlivePeriod(), activeSockets);
			ping.start();
		}
		
		System.out.println("Subscribe on: ws://" + InetAddress.getLocalHost().getHostAddress() + ":"
				+ properties.getWsPort() + properties.getSubscribePath());
	}

//	public void start() throws IOException {
//		// Start the server
//		server.start();
//	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {

		logger.debug("@onConnect");

		WebsocketListener listener = new WebsocketListener(conn, scheduler, activeSockets);

		synchronized (activeSockets) {
			activeSockets.put(conn, listener);
		}
		
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		if (ping != null)
			activeSockets.get(conn).unsubscribeAll();
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		new SubscribeHandler(scheduler, conn, message, activeSockets).start();
		
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
