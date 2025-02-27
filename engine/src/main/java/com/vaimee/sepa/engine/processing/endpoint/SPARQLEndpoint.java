package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.commons.request.QueryRequest;
import com.vaimee.sepa.commons.request.UpdateRequest;
import com.vaimee.sepa.commons.response.Response;

public interface SPARQLEndpoint {
	public Response query(QueryRequest req);
	public Response update(UpdateRequest req);
}
