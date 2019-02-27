package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.IOException;
import java.util.List;

import org.apache.commons.rdf.api.Dataset;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.timing.Timings;

/**
 * Table 6.2-1: Resources and HTTP methods defined on them
 * 
 * Resource Name: Entity by id Resource URI: /entities/{entityId} HTTP Method:
 * 1) GET 2) DELETE
 */
public class EntityById extends ResourceHandler {
	private boolean get = false;
	private int iteration = 0;
	private String entityId;

	public EntityById(Scheduler s, NgisLdHandlerBeans jmx) {
		super(s, "\\/entities\\/(?<entityId>.+)\\?(?<attrs>.+)|\\/entities\\/(?<id>.+[^?])", jmx);
	}

	private String buildConstruct(int it) {
		return "CONSTRUCT {" + buildConstructPattern(it) + "} WHERE { " + buildWherePattern(it) + " }";
	}

	private String buildConstructPattern(int it) {
		if (it == 0)
			return "?entity ?p0 ?o0";
		else
			return buildConstructPattern(it - 1) + " . ?o" + (it - 1) + " ?p" + it + " ?o" + it;
	}

	private String buildWherePattern(int it) {
		if (it == 0)
			return "{VALUES ?entity {<id>} ?entity ?p0 ?o0}";
		else
			return buildWherePattern(it - 1) + " UNION { " + buildConstructPattern(it) + " }";
	}

	private boolean noMoreBNodes(List<Bindings> bindings) {
		return true;
	}

	private Dataset buildDataset(List<Bindings> bindings) {
		return null;
	}

	@Override
	protected void delete(String link) {
		super.delete(link);
		get = false;

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
		get = true;

		// Get entity ID
		entityId = matcher.group("id");

		// TODO: implement query attributes (entityId + attrs)
		if (entityId == null)
			return;

		// Build the query
		iteration = 0;
		String query = buildConstruct(iteration).replace("id", entityId);
		logger.debug(query);

		// Schedule request
		InternalUQRequest sparql = new InternalQueryRequest(query, NgsiLdRdfMapper.ngsiLdEntitiesGraph,NgsiLdRdfMapper.ngsiLdEntitiesGraph);
		Timings.log(sparql);
		ScheduledRequest req = scheduler.schedule(sparql, this);

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
	public void sendResponse(Response response) throws SEPAProtocolException {
		if (get) {
			if (response.isError()) {
				ErrorResponse error = (ErrorResponse) response;
				setResponse(error.getStatusCode(), "application/json", NgsiLdError
						.buildResponse(NgsiLdError.InternalError, error.getError(), error.getErrorDescription()), null);
			} else {
				QueryResponse results = (QueryResponse) response;
				if (noMoreBNodes(results.getBindingsResults().getBindings())) {
					Dataset in = buildDataset(results.getBindingsResults().getBindings());
					String entityId = ngsiLdRdfMapper.getEntityUri(in);
					JsonObject frame = ngsiLdRdfMapper.getEntityFrame(entityId);
					
					try {
						setResponse(200, "application/ld+json", ngsiLdRdfMapper.toJsonLd(in, frame), null);
					} catch (IOException e) {
						setResponse(NgsiLdError.InternalError.getErrorCode(), "application/json", NgsiLdError.buildResponse(NgsiLdError.InternalError, "Exception", e.getMessage()), null);
						synchronized (matcher) {
							matcher.notify();
						}
					}
							
				} else {
					// Build the query
					iteration++;
					String query = buildConstruct(iteration).replace("id", entityId);
					logger.debug(query);

					// Schedule request
					InternalUQRequest sparql = new InternalQueryRequest(query,NgsiLdRdfMapper.ngsiLdEntitiesGraph,
							NgsiLdRdfMapper.ngsiLdEntitiesGraph);
					Timings.log(sparql);
					ScheduledRequest req = scheduler.schedule(sparql, this);

					// Request not scheduled
					if (req == null) {
						logger.error("Out of tokens");
						setResponse(NgsiLdError.TooManyPendingRequests.getErrorCode(), "application/json",
								NgsiLdError.buildResponse(NgsiLdError.TooManyPendingRequests,
										"Request cannot be scheduled", "Token not available"),
								null);
						jmx.outOfTokens();
						
						synchronized (matcher) {
							matcher.notify();
						}
						
						return;
					}
				}

			}
		} else {
			// TODO: to be implemented
		}
	}
}
