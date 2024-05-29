package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface SPARQLEndpoint {
	public Response query(QueryRequest req);
	public Response update(UpdateRequest req);
}
