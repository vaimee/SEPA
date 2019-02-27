package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.rdf.api.Dataset;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.timing.Timings;

/**
 * Table 6.2-1: Resources and HTTP methods defined on them
 * 
 * Resource Name: Entity List Resource URI: /entities/ HTTP Method: 1) POST ==>
 * Entity creation /entities/ 2) GET ==> Query entities /entities?query
 * 
 */
public class EntityList extends ResourceHandler {
	private String locationHeader = "";
	private String id;
	private String type;
	private JsonElement context;

	public EntityList(Scheduler s, NgisLdHandlerBeans jmx) {
		super(s, "\\/entities$|\\/entities\\/$|\\/entities(\\?)(?<query>.+)", jmx);
	}

	@Override
	protected void post(JsonObject body, String link) {
		super.post(body, link);

		// Query string must no be present
		if (matcher.group("query") != null)
			return;

		// Extract entity id, type and context
		id = body.get("@id").getAsString();
		type = body.get("@type").getAsString();
		context = body.get("@context");

		// Entity exists
		if (ngsiLdRdfMapper.entityExists(id)) {
			setResponse(NgsiLdError.AlreadyExists.getErrorCode(), "application/json",
					NgsiLdError.buildResponse(NgsiLdError.AlreadyExists, "Entity exits", body.toString()), null);
			return;
		}

		// From JSON-LD to RDF
		Dataset ds = null;
		try {
			ds = ngsiLdRdfMapper.fromJsonLd(body);
		} catch (IllegalStateException | IllegalArgumentException | InterruptedException | ExecutionException
				| TimeoutException | IOException e) {
			logger.error(e);
			setResponse(NgsiLdError.InternalError.getErrorCode(), "application/json",
					NgsiLdError.buildResponse(NgsiLdError.InternalError, "Exception", e.getMessage()), null);
			jmx.parsingFailed();
			return;
		}

		// SPARQL UPDATE
		String sparql = "INSERT DATA { GRAPH <" + NgsiLdRdfMapper.ngsiLdEntitiesGraph + "> {"
				+ ngsiLdRdfMapper.ntriples(ds) + "}}";
		locationHeader = "/ngsi-ld/v1/entities/" + ngsiLdRdfMapper.getEntityUri(ds);

		// Scheduler UPDATE request
		InternalUQRequest sepaRequest = new InternalUpdateRequest(sparql, NgsiLdRdfMapper.ngsiLdEntitiesGraph,
				NgsiLdRdfMapper.ngsiLdEntitiesGraph);
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest, this);

		// Request not scheduled
		if (req == null) {
			logger.error("Out of tokens");
			setResponse(NgsiLdError.TooManyPendingRequests.getErrorCode(), "application/json",
					NgsiLdError.buildResponse(NgsiLdError.TooManyPendingRequests, "Request cannot be scheduled",
							"Token not available"),
					null);
			jmx.outOfTokens();
			return;
		}

		// Wait for response
		try {
			synchronized (matcher) {
				matcher.wait();
			}
		} catch (InterruptedException e) {
			logger.error(e);
		}
	}

	@Override
	protected void get(String link) {
		super.get(link);

		if (matcher.group("query") == null || link == null)
			return;

		// TODO: to be implemented

	}

	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {
		Timings.log(response);
		if (matcher.group("query") == null) {
			if (response.isError()) {
				ErrorResponse error = (ErrorResponse) response;
				setResponse(error.getStatusCode(), "application/json", NgsiLdError
						.buildResponse(NgsiLdError.InternalError, error.getError(), error.getErrorDescription()), null);
			} else {
				if (ngsiLdRdfMapper.storeEntityFrame(id, type, context)) {
					Header location = new BasicHeader("Location", locationHeader);
					Header[] headers = new Header[1];
					headers[0] = location;
					setResponse(HttpStatus.SC_CREATED, null, null, headers);
				}
				else {
					setResponse(NgsiLdError.InternalError.getErrorCode(), "application/json", NgsiLdError.buildResponse(NgsiLdError.InternalError, "Failed to create entity context", "ID: "+id+" TYPE: "+type+" CONTEXT: "+context.toString()), null);	
				}
			}
		} else if (matcher.group("query") != null) {
			// TODO: to be implemented
		}
		synchronized (matcher) {
			matcher.notify();
		}
	}
}
