package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;

import org.apache.http.HttpStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.protocol.handler.SubscribeHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class SecureSubscribeServer extends WebSocketServer {
	private AuthorizationManager oauth;
	private Logger logger = LogManager.getLogger("SecureSubscribeServer");
	protected Scheduler scheduler;
	private KeepAlive ping = null;
	protected HashMap<WebSocket, WebsocketListener> activeSockets = new HashMap<WebSocket, WebsocketListener>();
	
	public SecureSubscribeServer(EngineProperties properties, Scheduler scheduler, AuthorizationManager oauth)
			throws IllegalArgumentException, UnknownHostException, KeyManagementException, NoSuchAlgorithmException {
		super( new InetSocketAddress( properties.getWssPort() ) );
		
		if (properties == null || scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");
		
		this.scheduler = scheduler;
		
		if (properties.getKeepAlivePeriod() > 0) {
			ping = new KeepAlive(properties.getKeepAlivePeriod(), activeSockets);
			ping.start();
		}
		
		this.oauth = oauth;

		setWebSocketFactory(new DefaultSSLWebSocketServerFactory(oauth.getSSLContext()));
		
		System.out.println("SECURE Subscribe on: wss://" + InetAddress.getLocalHost().getHostAddress() + ":"
				+ properties.getWssPort() + properties.getSecurePath()+properties.getSubscribePath());
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);

		// JWT Validation
		Response validation = validateToken(message);
		if (validation.getClass().equals(ErrorResponse.class)) {
			// Not authorized
			logger.warn("NOT AUTHORIZED");
			conn.send(validation.toString());
			return;
		}

		new SubscribeHandler(scheduler, conn, message, activeSockets).start();

	}

	private Response validateToken(String request) {
		JsonObject req;
		try {
			req = new JsonParser().parse(request).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {

			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, e.getMessage());
		}

		if (req.get("authorization") == null)
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "authorization key is missing");

		String oauthRequest = null;
		String jwt = null;
		try {
			oauthRequest = req.get("authorization").getAsString();
			if (!oauthRequest.startsWith("Bearer "))
				new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "authorization value MUST be of type Bearer");
			jwt = oauthRequest.substring(7);
		} catch (Exception e) {
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "authorization key value is wrong");
		}

		// Token validation
		return oauth.validateToken(jwt);
	}

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
		// TODO Auto-generated method stub
		
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
