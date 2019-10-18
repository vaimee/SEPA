package it.unibo.arces.wot.sepa.engine.gates;

import java.util.UUID;

import it.unibo.arces.wot.sepa.engine.scheduling.*;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.GateBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationResponse;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public abstract class Gate implements ResponseHandler, EventHandler {
	private static final Logger logger = LogManager.getLogger();

	protected final String gid;
	protected final Scheduler scheduler;
	private boolean authorizationRequired = false;

	public abstract void send(String response) throws SEPAProtocolException;

	public abstract boolean ping();

	public Gate(Scheduler scheduler) {
		this.scheduler = scheduler;

		gid = "sepa://gate/" + UUID.randomUUID();
	}
	
	protected final void enableAuthorization() {
		authorizationRequired = true;
	}

	public final String getGID() {
		return gid;
	}

	public final Scheduler getScheduler() {
		return scheduler;
	}

	public final void close() throws SEPAProcessingException {
		Dependability.onCloseGate(gid);
	}

	public final void onError(Exception e) {
		Dependability.onGateError(gid, e);
	}

	@Override
	public final void notifyEvent(Notification notify) throws SEPAProtocolException {
		logger.debug("@notifyEvent: " + notify);
		send(notify.toString());
		
		GateBeans.notification();
	}

	@Override
	public final void sendResponse(Response response) throws SEPAProtocolException {
		logger.debug("@sendResponse: " + response);
		send(response.toString());

		// JMX
		if (response.isSubscribeResponse()) {
			GateBeans.subscribeResponse();
		} else if (response.isUnsubscribeResponse()) {
			GateBeans.unsubscribeResponse();
		} else if (response.isError()) {
			GateBeans.errorResponse();
			logger.error(response);
		}
	}

	private static void setAliasIfPresent(ErrorResponse error, String message) {
		JsonObject request;
		try {
			request = new JsonParser().parse(message).getAsJsonObject();
		} catch (Exception e) {
			return;
		}

		JsonObject subUnsub = request.getAsJsonObject("subscribe");
		subUnsub = subUnsub == null ? request.getAsJsonObject("unsubscribe") : subUnsub;
		if (subUnsub.has("alias"))
			error.setAlias(subUnsub.get("alias").getAsString());
	}

	public final void onMessage(String message) throws SEPAProtocolException, SEPASecurityException {
		// Authorize the request
		AuthorizationResponse auth = authorize(message);
		if (!auth.isAuthorized()) {
			ErrorResponse error = new ErrorResponse(401, "auth_failed", auth.getError());
			setAliasIfPresent(error, message);
			sendResponse(error);
			return;
		}

		// Parse the request
		InternalRequest req = parseRequest(message, auth.getClientCredentials());
		if (req instanceof InternalDiscardRequest) {
			logger.error("@onMessage " + getGID() + " failed to parse message: " + message);
			setAliasIfPresent(((InternalDiscardRequest) req).getError(), message);
			sendResponse(((InternalDiscardRequest) req).getError());
			return;
		}

		// Schedule the request
		logger.debug("@onMessage: " + getGID() + " schedule request: " + req);
		ScheduledRequest request = scheduler.schedule(req, this);

		// Request not scheduled
		if (request == null) {
			logger.error("@onMessage: " + getGID() + " out of tokens");
			ErrorResponse response = new ErrorResponse(500, "too_many_requests", "Too many pending requests");
			setAliasIfPresent(response, message);
			sendResponse(response);
		}
	}

	/**
	 * <pre>
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
	
	According to RFC6749, the error member can assume the following values: invalid_request, invalid_client, invalid_grant, unauthorized_client, unsupported_grant_type, invalid_scope.
	
	     invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.
	 * 
	 * </pre>
	 * @throws SEPASecurityException 
	 */
	protected final AuthorizationResponse authorize(String message) throws SEPASecurityException {
		if (!authorizationRequired)
			return new AuthorizationResponse();

		JsonObject request;

		try {
			request = new JsonParser().parse(message).getAsJsonObject();	
		} catch (Exception e) {
			logger.error(e.getMessage());
			return new AuthorizationResponse("invalid_request","Failed to parse JSON message: "+message);
		}

		String bearer = null;
		JsonObject subUnsub = null;

		if (request.has("subscribe"))
			subUnsub = request.get("subscribe").getAsJsonObject();
		else if (request.has("unsubscribe"))
			subUnsub = request.get("unsubscribe").getAsJsonObject();

		if (subUnsub == null) {
			logger.error("Neither subscribe or unsuscribe found");
			return new AuthorizationResponse("invalid_request","Neither subscribe or unsuscribe found");
		}

		if (!subUnsub.has("authorization")) {
			logger.error("authorization member is missing");
			return new AuthorizationResponse("invalid_request","authorization member is missing");
		}

		try {
			bearer = subUnsub.get("authorization").getAsString();
		} catch (Exception e) {
			logger.error("Authorization member is not a string");
			return new AuthorizationResponse("invalid_request","authorization member is not a string");
		}

		if (!bearer.startsWith("Bearer ")) {
			logger.error("Authorization value MUST be of type Bearer");
			return new AuthorizationResponse("unsupported_grant_type","Authorization value MUST be of type Bearer");
		}

		String jwt = bearer.substring(7);

		if (jwt == null) {
			logger.error("Token is null");
			return new AuthorizationResponse("invalid_request","Token is null");
		}
		if (jwt.equals("")) {
			logger.error("Token is empty");
			return new AuthorizationResponse("invalid_request","Token is empty");
		}

		// Token validation
		return Dependability.validateToken(jwt);
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
	 * 
	 * @throws SEPAProtocolException
	 */
	protected final InternalRequest parseRequest(String request, Credentials credentials)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException,
			SEPAProtocolException {
		JsonObject req;
		ErrorResponse error;
		// TODO: Aggiungere new InternalDiscardRequest(request,(ErrorResponse) ret);
		try {
			req = new JsonParser().parse(request).getAsJsonObject();
		} catch (JsonParseException e) {
			error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "JsonParseException",
					"JsonParseException: " + request);
			return new InternalDiscardRequest(request, error, credentials);
		}

		if (req.has("subscribe")) {
			String sparql = null;
			String alias = null;
			String defaultGraphUri = null;
			String namedGraphUri = null;

			try {
				sparql = req.get("subscribe").getAsJsonObject().get("sparql").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception",
						"sparql member not found: " + request);
				return new InternalDiscardRequest(request, error, credentials);
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

			return new InternalSubscribeRequest(sparql, alias, defaultGraphUri, namedGraphUri, this, credentials);
		} else if (req.has("unsubscribe")) {
			String spuid;
			try {
				spuid = req.get("unsubscribe").getAsJsonObject().get("spuid").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception", "spuid member not found: " + request);
				return new InternalDiscardRequest(request, error, credentials);
			}

			return new InternalUnsubscribeRequest(gid, spuid, credentials);
		}

		error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unsupported", "Bad request: " + request);
		return new InternalDiscardRequest(request, error, credentials);
	}

}
