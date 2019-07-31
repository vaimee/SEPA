package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

public class ProcessorBeans {
	
	private static String host;
	private static int port;
	private static String queryPath;
	private static String updatePath;
	private static String updateMethod;
	private static String queryMethod;
	
	public static void setEndpoint(SPARQL11Properties prop) {
		host = prop.getHost();
		port = prop.getPort();
		queryPath = prop.getQueryPath();
		updatePath = prop.getUpdatePath();
		updateMethod = prop.getUpdateMethod().name();
		queryMethod = prop.getQueryMethod().name();
	}

	public static String getEndpointHost() {
		return host;
	}
	public static int getEndpointPort() {
		return port;
	}
	public static String getEndpointQueryPath() {
		return queryPath;
	}
	public static String getEndpointUpdatePath() {
		return updatePath;
	}
	public static String getEndpointUpdateMethod() {
		return updateMethod;
	}
	public static String getEndpointQueryMethod() {
		return queryMethod;
	}
}
