package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.net.UnknownHostException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class SecureWebsocketServer extends WebsocketServer implements SecureWebsocketServerMBean {
	private AuthorizationManager oauth;
	private Logger logger = LogManager.getLogger("SecureWebsocketServer");

	@Override
	protected String getWelcomeMessage() {
		return "Secure Subscribe     | wss://%s:%d%s";
	}

	public SecureWebsocketServer(int port, String path, Scheduler scheduler, AuthorizationManager oauth,int keepAlivePeriod,long timeout)
			throws IllegalArgumentException, UnknownHostException, KeyManagementException, NoSuchAlgorithmException {
		super(port, path, scheduler,keepAlivePeriod,timeout);

		if (oauth == null)
			throw new IllegalArgumentException("Authorization manager is null");

		this.oauth = oauth;

		setWebSocketFactory(new DefaultSSLWebSocketServerFactory(oauth.getSSLContext()));
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		logger.debug("@onMessage " + message);
		
		// JWT Validation
		Response validation = validateToken(message);
		if (validation.getClass().equals(ErrorResponse.class)) {
			// Not authorized
			jmx.onNotAuthorizedRequest();
			
			logger.warn("NOT AUTHORIZED");
			conn.send(validation.toString());
			return;
		}

		super.onMessage(conn, message);

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
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		super.onClose(conn, code, reason, remote);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		super.onError(conn, ex);
	}

	@Override
	public void onStart() {
		System.out.println(welcomeMessage);
		
		synchronized(this) {
			notify();
		}
	}
	
	@Override
	public long getNotAuthorized(){
		return jmx.getNotAuthorized();
	}

}
