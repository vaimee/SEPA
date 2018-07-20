package it.unibo.arces.wot.sepa.engine.scheduling;

public class InternalUpdateRequest extends InternalUQRequest {

	public InternalUpdateRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		super(sparql, defaultGraphUri, namedGraphUri);
	}

	@Override
	public String toString() {
		return "*UPDATE* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalUpdateRequest)) return false;
		return sparql.equals(((InternalUpdateRequest)obj).sparql);
	}
}
