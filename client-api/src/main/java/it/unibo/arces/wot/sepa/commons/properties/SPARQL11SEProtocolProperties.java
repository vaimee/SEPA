package it.unibo.arces.wot.sepa.commons.properties;

import java.util.HashMap;

public class SPARQL11SEProtocolProperties {
	public String protocol = null;
	public HashMap<String,SubscriptionProtocolProperties> availableProtocols = null;
	public String host = null;
	public boolean reconnect = true;

	public SPARQL11SEProtocolProperties merge(SPARQL11SEProtocolProperties temp) {
		if (temp != null) {
			protocol = (temp.protocol != null ? temp.protocol : protocol);
			host = (temp.host != null ? temp.host : host);
			reconnect = temp.reconnect;
			availableProtocols = (temp.availableProtocols != null ? temp.availableProtocols : availableProtocols);
		}
		return this;
	}

	public int getPort() {
		return availableProtocols.get(protocol).port;
	}

	public String getPath() {
		return availableProtocols.get(protocol).path;
	}

	public SubscriptionProtocolProperties getSubscriptionProtocol() {
		return availableProtocols.get(protocol);
	}
}
