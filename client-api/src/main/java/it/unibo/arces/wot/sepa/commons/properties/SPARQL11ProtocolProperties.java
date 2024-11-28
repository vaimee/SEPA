package it.unibo.arces.wot.sepa.commons.properties;

import it.unibo.arces.wot.sepa.commons.properties.SPARQL11Properties.ProtocolScheme;

public class SPARQL11ProtocolProperties {
	private String host = null;
	private ProtocolScheme protocol = null;
	private int port = -1;
	private QueryProperties query = new QueryProperties();
	private UpdateProperties update = new UpdateProperties();
	
	public SPARQL11ProtocolProperties merge(SPARQL11ProtocolProperties temp) {
		if (temp != null) {
			this.setHost((temp.getHost() != null ? temp.getHost() : this.getHost()));
			this.setProtocol((temp.getProtocol() != null ? temp.getProtocol() : this.getProtocol()));
			this.setPort((temp.getPort() != -1 ? temp.getPort() : this.getPort()));
			this.getQuery().merge(temp.getQuery());
			this.getUpdate().merge(temp.getUpdate());
		} 
		
		return this;
	}

	public ProtocolScheme getProtocol() {
		return protocol;
	}

	public void setProtocol(ProtocolScheme protocol) {
		this.protocol = protocol;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public UpdateProperties getUpdate() {
		return update;
	}

	public void setUpdate(UpdateProperties update) {
		this.update = update;
	}

	public QueryProperties getQuery() {
		return query;
	}

	public void setQuery(QueryProperties query) {
		this.query = query;
	}
	
}
