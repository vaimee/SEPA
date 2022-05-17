package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.logging.Logging;

public class RemoteEndpoint implements SPARQLEndpoint {
	SPARQL11Protocol endpoint;

	public RemoteEndpoint() {
		try {
			endpoint = new SPARQL11Protocol();
		} catch (SEPASecurityException e) {
			Logging.logger.error(e.getMessage());
		}
	}

	@Override
	public Response query(QueryRequest req,SEPAUserInfo usr) {
		return endpoint.query(req);
	}

	@Override
	public Response update(UpdateRequest req,SEPAUserInfo usr) {
		return endpoint.update(req);
	}

	@Override
	public void close() {
		try {
			endpoint.close();
		} catch (IOException e) {
			Logging.logger.error(e.getMessage());
		}
	}

}
