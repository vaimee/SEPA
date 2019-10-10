package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;

public class InternalQueryRequest extends InternalUQRequest {

	public InternalQueryRequest(String sparql, String defaultGraphUri, String namedGraphUri,ClientCredentials credentials) {
		super(sparql, defaultGraphUri, namedGraphUri,credentials);
	}

	@Override
	public String toString() {
		return "*QUERY* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
}
