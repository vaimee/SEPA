package it.unibo.arces.wot.sepa.commons.properties;

import it.unibo.arces.wot.sepa.commons.properties.SPARQL11Properties.ProtocolScheme;

public class SPARQL11ProtocolProperties {
	public String host = null;
	public ProtocolScheme protocol = null;
	public int port = -1;
	public QueryProperties query = null;
	public UpdateProperties update = null;
	
	public SPARQL11ProtocolProperties merge(SPARQL11ProtocolProperties temp) {
		if (temp != null) {
			this.host = (temp.host != null ? temp.host : this.host);
			this.protocol = (temp.protocol != null ? temp.protocol : this.protocol);
			this.port = (temp.port != -1 ? temp.port : this.port);
			this.query.merge(temp.query);
			this.update.merge(temp.update);
		} 
		
		return this;
	}

//	public String getUpdateAcceptHeader() {
//		return update.format.getUpdateAcceptHeader();
//	}
//
//	public UpdateHTTPMethod getUpdateMethod() {
//		return update.method;
//	}
//
//	public String getProtocolScheme() {
//		return protocol.getProtocolScheme();
//	}
//
//	public String getUpdatePath() {
//		return update.path;
//	}
	
}
