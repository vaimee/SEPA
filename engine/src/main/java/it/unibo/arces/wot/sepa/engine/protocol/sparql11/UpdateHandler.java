package it.unibo.arces.wot.sepa.engine.protocol.sparql11;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationResponse;
import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class UpdateHandler extends SPARQL11Handler {
	protected static final Logger logger = LogManager.getLogger();

	public UpdateHandler(Scheduler scheduler) throws IllegalArgumentException {
		super(scheduler);
	}

	@Override
	protected InternalUQRequest parse(HttpAsyncExchange exchange,ClientCredentials credentials) {
		if (!exchange.getRequest().getRequestLine().getMethod().toUpperCase().equals("POST")) {
			logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST,
					"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		}
		
		return parsePost(exchange,"update",credentials);
	}
	
	@Override
	protected AuthorizationResponse authorize(HttpRequest request) {
		return new AuthorizationResponse();
	}
}
