package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;

public class RemoteEndpoint implements SPARQLEndpoint {
	protected static final Logger logger = LogManager.getLogger();
	
	SPARQL11Protocol endpoint;

	public RemoteEndpoint() {
		try {
			endpoint = new SPARQL11Protocol();
		} catch (SEPASecurityException e) {
			logger.error(e.getMessage());
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
			logger.error(e.getMessage());
		}
	}

}
