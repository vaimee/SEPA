package it.unibo.arces.wot.sepa.commons.request;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;

public abstract class SPARQL11Request extends Request {
	protected HTTPMethod method = HTTPMethod.POST;
	protected String scheme = null;
	protected String host = null;
	protected int port = -1;
	protected String path = null;
	
	protected String default_graph_uri = null;
	protected String named_graph_uri = null;
	
	public SPARQL11Request(String sparql, String auth,String defaultGraphUri,String namedGraphUri,long timeout) {
		super(sparql, auth,timeout);
		
		this.default_graph_uri = defaultGraphUri;
		this.named_graph_uri = namedGraphUri;
	}

	@Override
	public String toString() {
		return sparql;
		
	}
	
	public HTTPMethod getHttpMethod() {
		return method;
	}

	public String getScheme() {
		return scheme;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}
	
	public String getDefaultGraphUri() {
		return default_graph_uri;
	}

	public String getNamedGraphUri() {
		return named_graph_uri;
	}
}
