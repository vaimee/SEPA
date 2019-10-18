package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public class InternalDataUpdateRequest extends InternalUpdateRequest {

	public InternalDataUpdateRequest(String sparql, String defaultGraphUri, String namedGraphUri,Credentials credentials) {
		super(sparql, defaultGraphUri, namedGraphUri,credentials);
	}

}
