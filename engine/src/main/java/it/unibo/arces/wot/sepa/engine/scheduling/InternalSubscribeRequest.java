package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class InternalSubscribeRequest extends InternalQueryRequest {

	private String alias = null;
	private EventHandler handler;
	
	public InternalSubscribeRequest(String sparql, String alias,String defaultGraphUri, String namedGraphUri,EventHandler handler) {
		super(sparql, defaultGraphUri, namedGraphUri);
		
		this.alias = alias;
		this.handler = handler;
	}
	
	@Override
	public String toString() {
		return "*SUBSCRIBE* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
	
	public String getSpuid() {
		return sparql;
	}
	
	public String getAlias() {
		return alias;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalSubscribeRequest)) return false;
		return sparql.equals(((InternalSubscribeRequest)obj).sparql);
	}
	
	public EventHandler getEventHandler() {
		return handler;
	}
}
