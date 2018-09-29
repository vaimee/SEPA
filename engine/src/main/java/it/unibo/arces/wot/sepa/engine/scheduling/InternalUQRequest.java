package it.unibo.arces.wot.sepa.engine.scheduling;

public abstract class InternalUQRequest extends InternalRequest {
	protected String sparql;
	protected String defaultGraphUri;
	protected String namedGraphUri;
	
	public InternalUQRequest(String sparql,String defaultGraphUri,String namedGraphUri) {
		if (sparql == null) throw new IllegalArgumentException("SPARQL is null");
		
		this.sparql = sparql;
		this.defaultGraphUri = defaultGraphUri;
		this.namedGraphUri = namedGraphUri;
	}
	
	public String getSparql() {
		return sparql;
	}
	
	public String getDefaultGraphUri() {
		return defaultGraphUri;
	}
	
	public String getNamedGraphUri() {
		return namedGraphUri;
	}
	
	@Override
	public int hashCode() {
		return sparql.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return sparql.equals(((InternalUQRequest)obj).sparql);
	}
}
