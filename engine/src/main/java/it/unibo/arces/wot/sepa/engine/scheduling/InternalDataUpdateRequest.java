package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;

public class InternalDataUpdateRequest extends InternalUpdateRequest {

	public InternalDataUpdateRequest(String sparql, String defaultGraphUri, String namedGraphUri,ClientCredentials credentials) {
		super(sparql, defaultGraphUri, namedGraphUri,credentials);
	}

}
