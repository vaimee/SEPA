package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.request.QueryRequest;
import com.vaimee.sepa.api.commons.request.UpdateRequest;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.engine.dependability.acl.SEPAUserInfo;

public class RemoteEndpoint implements SPARQLEndpoint {
	SPARQL11Protocol endpoint;

	public RemoteEndpoint() throws SEPASecurityException {
		endpoint = new SPARQL11Protocol();
	}

	@Override
	public Response query(QueryRequest req, SEPAUserInfo usr) {
		return query(req);
	}

	@Override
	public Response update(UpdateRequest req, SEPAUserInfo usr) {
		return update(req);
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
