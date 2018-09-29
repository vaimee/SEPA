package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

public class ProcessorBeans {
	
	private static String host;
	private static int port;
	private static String queryPath;
	private static String updatePath;
	private static String updateMethod;
	private static String queryMethod;
	
	private static int maxConcurrentRequests;
	
	public static void setEndpoint(SPARQL11Properties prop,int maxReq) {
		host = prop.getDefaultHost();
		port = prop.getDefaultPort();
		queryPath = prop.getDefaultQueryPath();
		updatePath = prop.getUpdatePath();
		updateMethod = prop.getUpdateMethod().name();
		queryMethod = prop.getQueryMethod().name();
		
		maxConcurrentRequests = maxReq;
	}
	
	public static int getMaxConcurrentRequests() {
		return maxConcurrentRequests;
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
