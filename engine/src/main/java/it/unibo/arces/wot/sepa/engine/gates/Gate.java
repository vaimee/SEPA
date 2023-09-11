/* An abstract gate for the SPARQL 1.1 Subscribe requests management
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package it.unibo.arces.wot.sepa.engine.gates;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import it.unibo.arces.wot.sepa.engine.scheduling.*;
import it.unibo.arces.wot.sepa.logging.Logging;

import org.apache.http.HttpStatus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.bean.GateBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;

public abstract class Gate implements ResponseHandler, EventHandler {

	protected final String gid;
	protected final Scheduler scheduler;
	private boolean authorizationRequired = false;

	public abstract void send(Response response) throws SEPAProtocolException;

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

	public final void close() throws InterruptedException {
		Dependability.onCloseGate(gid);
	}

	public final void onError(Exception e) {
		Dependability.onGateError(gid, e);
	}

	@Override
	public final void notifyEvent(Notification notify) throws SEPAProtocolException {
		Logging.logger.trace("@notifyEvent: " + notify);
		send(notify);
		
		GateBeans.notification();
	}

	@Override
	public final void sendResponse(Response response) throws SEPAProtocolException {
		Logging.logger.trace("@sendResponse: " + response);
		send(response);

		// JMX
		if (response.isSubscribeResponse()) {
			GateBeans.subscribeResponse();
		} else if (response.isUnsubscribeResponse()) {
			GateBeans.unsubscribeResponse();
		} else if (response.isError()) {
			GateBeans.errorResponse();
			Logging.logger.error(response);
		}
	}

	private static void setAliasIfPresent(ErrorResponse error, String message) {
		JsonObject request;
		try {
			request = new Gson().fromJson(message,JsonObject.class);
		} catch (Exception e) {
			return;
		}

		JsonObject subUnsub = request.getAsJsonObject("subscribe");
		subUnsub = subUnsub == null ? request.getAsJsonObject("unsubscribe") : subUnsub;
		if (subUnsub.has("alias"))
			error.setAlias(subUnsub.get("alias").getAsString());
	}

	public final void onMessage(String message) throws SEPAProtocolException, SEPASecurityException, SEPASparqlParsingException {
		// Authorize the request
		ClientAuthorization auth = authorize(message);
		
		if (!auth.isAuthorized()) {
			ErrorResponse error = new ErrorResponse(401, auth.getError(), auth.getDescription());
			setAliasIfPresent(error, message);
			sendResponse(error);
			return;
		}

		// Parse the request
		InternalRequest req = parseRequest(message, auth);
		if (req instanceof InternalDiscardRequest) {
			Logging.logger.error("@onMessage " + getGID() + " failed to parse message: " + message);
			setAliasIfPresent(((InternalDiscardRequest) req).getError(), message);
			sendResponse(((InternalDiscardRequest) req).getError());
			return;
		}

		// Schedule the request
		Logging.logger.trace("@onMessage: " + getGID() + " schedule request: " + req);
		ScheduledRequest request = scheduler.schedule(req, this);

		// Request not scheduled
		if (request == null) {
			Logging.logger.error("@onMessage: " + getGID() + " out of tokens");
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
	protected final ClientAuthorization authorize(String message) throws SEPASecurityException {
		if (!authorizationRequired)
			return new ClientAuthorization();

		JsonObject request;

		try {
			request = new Gson().fromJson(message,JsonObject.class);	
		} catch (Exception e) {
			Logging.logger.error(e.getMessage());
			return new ClientAuthorization("invalid_request","Failed to parse JSON message: "+message);
		}

		String bearer = null;
		JsonObject subUnsub = null;

		if (request.has("subscribe"))
			subUnsub = request.get("subscribe").getAsJsonObject();
		else if (request.has("unsubscribe"))
			subUnsub = request.get("unsubscribe").getAsJsonObject();

		if (subUnsub == null) {
			Logging.logger.error("Neither subscribe or unsuscribe found");
			return new ClientAuthorization("invalid_request","Neither subscribe or unsuscribe found");
		}

		if (!subUnsub.has("authorization")) {
			Logging.logger.error("authorization member is missing");
			return new ClientAuthorization("invalid_request","authorization member is missing");
		}

		try {
			bearer = subUnsub.get("authorization").getAsString();
		} catch (Exception e) {
			Logging.logger.error("Authorization member is not a string");
			return new ClientAuthorization("invalid_request","authorization member is not a string");
		}

		if (!bearer.toUpperCase().startsWith("BEARER ")) {
			Logging.logger.error("Authorization value MUST be of type Bearer");
			return new ClientAuthorization("unsupported_grant_type","Authorization value MUST be of type Bearer");
		}

		String jwt = bearer.substring(7);

		if (jwt == null) {
			Logging.logger.error("Token is null");
			return new ClientAuthorization("invalid_request","Token is null");
		}
		if (jwt.equals("")) {
			Logging.logger.error("Token is empty");
			return new ClientAuthorization("invalid_request","Token is empty");
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
	protected final InternalRequest parseRequest(String request, ClientAuthorization auth)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException,
			SEPAProtocolException, SEPASparqlParsingException {
		JsonObject req;
		ErrorResponse error;
		
		try {
			req = new Gson().fromJson(request,JsonObject.class);
		} catch (JsonParseException e) {
			error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "JsonParseException",
					"JsonParseException: " + request);
			return new InternalDiscardRequest(request, error, auth);
		}

		if (req.has("subscribe")) {
			String sparql = null;
			String alias = null;
			Set<String> defaultGraphUri = new HashSet<String>();
			Set<String> namedGraphUri = new HashSet<String>();

			try {
				sparql = req.get("subscribe").getAsJsonObject().get("sparql").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception",
						"sparql member not found: " + request);
				return new InternalDiscardRequest(request, error, auth);
			}

			try {
				alias = req.get("subscribe").getAsJsonObject().get("alias").getAsString();
			} catch (Exception e) {
			}

			try {
				JsonArray array = req.get("subscribe").getAsJsonObject().get("default-graph-uri").getAsJsonArray();
				for (JsonElement element : array) defaultGraphUri.add(element.getAsString());
			} catch (Exception e) {
			}

			try {
				JsonArray array = req.get("subscribe").getAsJsonObject().get("named-graph-uri").getAsJsonArray();
				for (JsonElement element : array) namedGraphUri.add(element.getAsString());
			} catch (Exception e) {
			}

			return new InternalSubscribeRequest(sparql, alias, defaultGraphUri, namedGraphUri, this, auth);
		} else if (req.has("unsubscribe")) {
			String spuid;
			try {
				spuid = req.get("unsubscribe").getAsJsonObject().get("spuid").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception", "spuid member not found: " + request);
				return new InternalDiscardRequest(request, error, auth);
			}

			return new InternalUnsubscribeRequest(gid, spuid, auth);
		}

		error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unsupported", "Bad request: " + request);
		return new InternalDiscardRequest(request, error, auth);
	}

}
