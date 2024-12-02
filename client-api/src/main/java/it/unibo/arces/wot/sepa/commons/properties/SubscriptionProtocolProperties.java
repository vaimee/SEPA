package it.unibo.arces.wot.sepa.commons.properties;

public class SubscriptionProtocolProperties {
	private String path = null;
	private String scheme = null;
	private int port = -1;

	public SubscriptionProtocolProperties(String scheme,int port,String path) {
		this.scheme = scheme;
		this.port = port;
		this.path = path;
	}

	public SubscriptionProtocolProperties() {
		scheme = "ws";
		port = 9000;
		path = "/subscribe";
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getScheme() {
		return scheme;
	}
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
}


