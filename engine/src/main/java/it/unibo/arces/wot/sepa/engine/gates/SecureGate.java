package it.unibo.arces.wot.sepa.engine.gates;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public abstract class SecureGate extends Gate{
	private static final Logger logger = LogManager.getLogger();
	
	public SecureGate(Scheduler scheduler) {
		super(scheduler);
	}

	@Override
	protected InternalRequest parseRequest(String request)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException, SEPAProtocolException {
		JsonObject req;

		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(Exception e) {
			ErrorResponse error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception","Exception: " + request);
			send(error.toString());
			logger.error(error);
			return null;
		}
		
		// CHECK AUTHORIZATION
		Response ret = validateRequest(req);
		
		if (ret.isError()) {
			// Not authorized
			logger.warn("NOT AUTHORIZED");
			send(ret.toString());
			return null;
		}
		
		return super.parseRequest(request);
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
		return Dependability.validateToken(jwt);
	}
}
