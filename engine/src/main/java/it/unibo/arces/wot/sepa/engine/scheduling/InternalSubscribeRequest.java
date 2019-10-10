package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;

public class InternalSubscribeRequest extends InternalQueryRequest {

	private String alias = null;
	private EventHandler gate;
	
	public InternalSubscribeRequest(String sparql, String alias,String defaultGraphUri, String namedGraphUri,EventHandler gate,ClientCredentials credentials) {
		super(sparql, defaultGraphUri, namedGraphUri,credentials);
		
		this.alias = alias;
		this.gate = gate;
	}
	
	@Override
	public String toString() {
		return "*SUBSCRIBE* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
	
	public String getAlias() {
		return alias;
	}
	
	public EventHandler getEventHandler() {
		return gate;
	}
}
