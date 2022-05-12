package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import java.io.IOException;

import it.unibo.arces.wot.sepa.engine.acl.SEPAUserInfo;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.timing.Timings;

public class RemoteEndpoint implements SPARQLEndpoint {
	SPARQL11ProtocolException endpoint;

	public RemoteEndpoint() {
		try {
			endpoint = new SPARQL11ProtocolException();
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
