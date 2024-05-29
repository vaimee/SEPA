package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.api.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class RemoteEndpoint implements SPARQLEndpoint {
	SPARQL11Protocol endpoint;

	public RemoteEndpoint() throws SEPASecurityException {
		endpoint = new SPARQL11Protocol();
	}

	@Override
	public Response query(QueryRequest req) {
		return endpoint.query(req);
	}

	@Override
	public Response update(UpdateRequest req) {
		return endpoint.update(req);
	}

}
