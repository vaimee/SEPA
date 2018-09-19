package it.arces.wot.sepa.engine.gates;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public abstract class Gate implements ResponseHandler, EventHandler {
	private static final Logger logger = LogManager.getLogger();

	protected final String gid;
	protected final Scheduler scheduler;

	public abstract void send(String response) throws SEPAProtocolException;

	public Gate(Scheduler scheduler) {
		this.scheduler = scheduler;
		
		gid = "sepa://gate/" + UUID.randomUUID();
	}

	public final String getGID() {
		return gid;
	}

	public final Scheduler getScheduler() {
		return scheduler;
	}

	public final void close() {
		Dependability.onCloseGate(gid);
	}
	
	public final void onError(Exception e) {
		Dependability.onGateError(gid,e);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		logger.trace("@notifyEvent: " + notify);
		send(notify.toString());
	}

	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		logger.debug("@sendResponse: " + response);
		send(response.toString());
	}

	public final void onMessage(String message) throws SEPAProtocolException {
		// Parse the request
		InternalRequest req = parseRequest(message);
		if (req == null) {
			logger.error("@onMessage " + getGID() + " failed to parse message: " + req);
			ErrorResponse response = new ErrorResponse(400, "parsing_error", "Malformed request: " + req);
			sendResponse(response);
			return;
		}

		// Schedule the request
		logger.debug("@onMessage: " + getGID() + " schedule request: " + req);
		ScheduledRequest request = scheduler.schedule(req, this);

		// Request not scheduled
		if (request == null) {
			logger.error("@onMessage: " + getGID() + " out of tokens");
			ErrorResponse response = new ErrorResponse(500, "too_many_requests", "Too many pending requests");
			sendResponse(response);
		}
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
	protected InternalRequest parseRequest(String request) throws JsonParseException, JsonSyntaxException,
			IllegalStateException, ClassCastException, SEPAProtocolException {
		JsonObject req;
		ErrorResponse error;

		try {
			req = new JsonParser().parse(request).getAsJsonObject();
		} catch (JsonParseException e) {
			error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "JsonParseException",
					"JsonParseException: " + request);
			sendResponse(error);
			logger.error(error);
			return null;
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
				sendResponse(error);
				logger.error(error);
				return null;
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

			return new InternalSubscribeRequest(sparql, alias, defaultGraphUri, namedGraphUri, this);
		} else if (req.has("unsubscribe")) {
			String spuid;
			try {
				spuid = req.get("unsubscribe").getAsJsonObject().get("spuid").getAsString();
			} catch (Exception e) {
				error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Exception", "spuid member not found: " + request);
				sendResponse(error);
				return null;
			}

			return new InternalUnsubscribeRequest(gid,spuid);
		}

		error = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unsupported", "Bad request: " + request);
		sendResponse(error);
		return null;
	}

}
