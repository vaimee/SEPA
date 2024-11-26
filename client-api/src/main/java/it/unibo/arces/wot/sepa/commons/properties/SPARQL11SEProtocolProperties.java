package it.unibo.arces.wot.sepa.commons.properties;

import java.util.HashMap;

public class SPARQL11SEProtocolProperties {
	private String protocol = null;
	private HashMap<String,SubscriptionProtocolProperties> availableProtocols = new HashMap<String,SubscriptionProtocolProperties>();
	private String host = null;
	private boolean reconnect = true;

	public SPARQL11SEProtocolProperties merge(SPARQL11SEProtocolProperties temp) {
		if (temp != null) {
			setProtocol((temp.getProtocol() != null ? temp.getProtocol() : getProtocol()));
			setHost((temp.getHost() != null ? temp.getHost() : getHost()));
			setReconnect(temp.isReconnect());
			setAvailableProtocols((temp.getAvailableProtocols() != null ? temp.getAvailableProtocols() : getAvailableProtocols()));
		}
		return this;
	}

	public int getPort() {
		return getAvailableProtocols().get(getProtocol()).getPort();
	}

	public String getPath() {
		return getAvailableProtocols().get(getProtocol()).getPath();
	}

	public SubscriptionProtocolProperties getSubscriptionProtocol() {
		return getAvailableProtocols().get(getProtocol());
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean isReconnect() {
		return reconnect;
	}

	public void setReconnect(boolean reconnect) {
		this.reconnect = reconnect;
	}

	public HashMap<String,SubscriptionProtocolProperties> getAvailableProtocols() {
		return availableProtocols;
	}

	public void setAvailableProtocols(HashMap<String,SubscriptionProtocolProperties> availableProtocols) {
		this.availableProtocols = availableProtocols;
	}
}
