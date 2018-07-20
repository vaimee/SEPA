package it.unibo.arces.wot.sepa.engine.protocol.websocket;

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
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityManager;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SecureWebsocketServer extends WebsocketServer implements SecureWebsocketServerMBean {
	private AuthorizationManager oauth;
	private final static Logger logger = LogManager.getLogger();

	@Override
	protected String getWelcomeMessage() {
		return "SPARQL 1.1 Subscribe | wss://%s:%d%s";
	}

	public SecureWebsocketServer(int port, String path, Scheduler scheduler, AuthorizationManager oauth, DependabilityManager dependabilityMng)
			throws SEPAProtocolException, SEPASecurityException {
		super(port, path, scheduler, dependabilityMng);

		if (oauth == null)
			throw new IllegalArgumentException("Authorization manager is null");

		this.oauth = oauth;

		try {
			setWebSocketFactory(new DefaultSSLWebSocketServerFactory(oauth.getSSLContext()));
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			logger.error(e.getMessage());
			throw new SEPASecurityException(e);
		}
	}

	@Override
	protected InternalRequest parseRequest(String request, WebSocket conn)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;

		try {
			req = new JsonParser().parse(request).getAsJsonObject();
			
			if (req.has("subscribe")) {
				String auth = req.get("subscribe").getAsJsonObject().get("authorization").getAsString();
				Response ret = validateToken(auth);
				if (ret.isError()) {
					// Not authorized
					WebsocketBeans.onNotAuthorizedRequest();

					logger.warn("NOT AUTHORIZED");
					conn.send(ret.toString());
					return null;
				}
				
				String sparql = null;
				String alias = null;
				String defaultGraphUri = null;
				String namedGraphUri = null;
				
				try {
					sparql = req.get("subscribe").getAsJsonObject().get("sparql").getAsString();
				}
				catch(Exception e) {
					logger.error("SPARQL member not found");
					return null;
				}
				
				try {
					alias = req.get("subscribe").getAsJsonObject().get("alias").getAsString();
				}
				catch(Exception e) {}
				
				try {
					defaultGraphUri = req.get("subscribe").getAsJsonObject().get("default-graph-uri").getAsString();
				}
				catch(Exception e) {}
				
				try {
					namedGraphUri = req.get("subscribe").getAsJsonObject().get("named-graph-uri").getAsString();
				}
				catch(Exception e) {}
				
				return new InternalSubscribeRequest(sparql,alias,defaultGraphUri,namedGraphUri,activeSockets.get(conn));
			}
			else if (req.has("unsubscribe")) {
				Response ret = validateToken(
						req.get("unsubscribe").getAsJsonObject().get("authorization").getAsString());
				if (ret.isError()) {
					// Not authorized
					WebsocketBeans.onNotAuthorizedRequest();

					logger.warn("NOT AUTHORIZED");
					conn.send(ret.toString());
					return null;
				}
				return new InternalUnsubscribeRequest(req.get("unsubscribe").getAsJsonObject().get("spuid").getAsString());
			}
		} catch (Exception e) {
			logger.debug(e.getLocalizedMessage());
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
			conn.send(response.toString());
			return null;
		}
		
		logger.debug("Unknown request: "+request);
		ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Unknown request: "+request);
		conn.send(response.toString());
		return null;
	}

	private Response validateToken(String bearer) {
		String jwt = null;
		try {
			if (!bearer.startsWith("Bearer "))
				new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Authorization value MUST be of type Bearer");
			jwt = bearer.substring(7);
		} catch (Exception e) {
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "Authorization key value is wrong");
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

		synchronized (this) {
			notify();
		}
	}

	@Override
	public long getNotAuthorized() {
		return WebsocketBeans.getNotAuthorized();
	}

}
