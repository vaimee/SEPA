package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.request.QueryRequest;
import com.vaimee.sepa.commons.request.UpdateRequest;
import com.vaimee.sepa.commons.response.Response;

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
