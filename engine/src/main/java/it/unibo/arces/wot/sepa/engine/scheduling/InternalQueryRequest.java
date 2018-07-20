package it.unibo.arces.wot.sepa.engine.scheduling;

public class InternalQueryRequest extends InternalUQRequest {

	public InternalQueryRequest(String sparql, String defaultGraphUri, String namedGraphUri) {
		super(sparql, defaultGraphUri, namedGraphUri);
	}

	@Override
	public String toString() {
		return "*QUERY* "+sparql + " DEFAULT GRAPH URI: <"+defaultGraphUri + "> NAMED GRAPH URI: <" + namedGraphUri+">";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalQueryRequest)) return false;
		return sparql.equals(((InternalQueryRequest)obj).sparql);
	}
}
