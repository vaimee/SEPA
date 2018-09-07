package it.unibo.arces.wot.sepa.engine.protocol.websocket;

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

		setWebSocketFactory(new DefaultSSLWebSocketServerFactory(oauth.getSSLContext()));
	}

	@Override
	protected InternalRequest parseRequest(String request, WebSocket conn)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;

		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(Exception e) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception","Exception: " + request);
			conn.send(error.toString());
			logger.error(error);
			return null;
		}
		
		// CHECK AUTHORIZATION
		Response ret = validateRequest(req);
		
		if (ret.isError()) {
			// Not authorized
			WebsocketBeans.onNotAuthorizedRequest();

			logger.warn("NOT AUTHORIZED");
			conn.send(ret.toString());
			return null;
		}
		
		return super.parseRequest(request, conn);
	}
/**
	<pre>
	Specific to SPARQL 1.1 SE Subscribe request:
	1. Check if the request contains an "authorization" member. 
	2. Check if the request contains an "authorization" member that start with "Bearer" 
	3. Check if the value of the "authorization" member is a JWT object ==> VALIDATE TOKEN

	Token validation:
	4. Check if the JWT object is signed 
	5. Check if the signature of the JWT object is valid. This is to be checked with AS public signature verification key 
	6. Check the contents of the JWT object 
	7. Check if the value of "iss" is https://wot.arces.unibo.it:8443/oauth/token 
	8. Check if the value of "aud" contains https://wot.arces.unibo.it:8443/sparql 
	9. Accept the request as well as "sub" as the originator of the request and process it as usual
	 
	Respond with 401 if not
	 
	</pre>
	*/
	private Response validateRequest(JsonObject request) {
		String bearer = null;
		JsonObject subUnsub = null;
		
		if (request.has("subscribe")) subUnsub = request.get("subscribe").getAsJsonObject();
		else if (request.has("unsubscribe")) subUnsub = request.get("unsubscribe").getAsJsonObject();
		
		if (subUnsub == null) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "invalid_request","Neither subscribe or unsuscribe found");	
			logger.error(error);
			return error;	
		}
		
		if (!subUnsub.has("authorization")) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "unauthorized_client","Authorization member is missing");	
			logger.error(error);
			return error;
		}
		
		try{
			bearer = subUnsub.get("authorization").getAsString();
		}
		catch(Exception e) {
			ErrorResponse error =  new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "unauthorized_client","Authorization member is not a string");	
			logger.error(error);
			return error;
		}
		
		if (!bearer.startsWith("Bearer ")) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "unauthorized_client","Authorization value MUST be of type Bearer");
			logger.error(error);
			return error;
		}
		
		String jwt = bearer.substring(7);
		
		if (jwt == null) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"unauthorized_client", "Token is null");
			logger.error(error);
			return error;
		}
		if (jwt.equals("")) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"unauthorized_client", "Token is empty");
			logger.error(error);
			return error;
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
