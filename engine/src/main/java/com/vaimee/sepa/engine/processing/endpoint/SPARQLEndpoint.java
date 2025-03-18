package com.vaimee.sepa.engine.processing.endpoint;

import com.vaimee.sepa.commons.request.QueryRequest;
import com.vaimee.sepa.commons.request.UpdateRequest;
import com.vaimee.sepa.commons.response.Response;
import com.vaimee.sepa.engine.dependability.acl.SEPAUserInfo;

public interface SPARQLEndpoint {
	Response query(QueryRequest req, SEPAUserInfo usr);
	Response update(UpdateRequest req, SEPAUserInfo usr);

	default Response query(QueryRequest req) {
		return query(req,null);
	}
	default Response update(UpdateRequest req) {
		return update(req,null);
	}
}
