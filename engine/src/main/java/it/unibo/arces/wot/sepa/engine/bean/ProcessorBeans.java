package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;

public class ProcessorBeans {
	private static int totalQueryRequests = 0;
	private static int totalRequests = 0;
	
	private static int updateRequests = 0;
	private static float queryMinTime = -1;
	private static float queryAverageTime = -1;
	private static float queryMaxTime = -1;
	private static float queryTime = -1;
	
	private static int queryRequests = 0 ;	
	private static float updateMinTime = -1;
	private static float updateAverageTime = -1;
	private static float updateMaxTime = -1;
	private static float updateTime = -1;
	
	// Endpoint properties
	private static String host;
	private static int port;
	private static String queryPath;
	private static String updatePath;
	private static String updateMethod;
	private static String queryMethod;
	
	public static void setEndpoint(SPARQL11Properties prop) {
		host = prop.getHost();
		port = prop.getHttpPort();
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
	
 	public static void updateTimings(long start, long stop) {
		updateTime = stop - start;

		updateRequests++;
		
		if (updateMinTime == -1)
			updateMinTime = updateTime;
		else if (updateTime < updateMinTime)
			updateMinTime = updateTime;

		if (updateMaxTime == -1)
			updateMaxTime = updateTime;
		else if (updateTime > updateMaxTime)
			updateMaxTime = updateTime;

		if (updateAverageTime == -1)
			updateAverageTime = updateTime;
		else
			updateAverageTime = ((updateAverageTime * (updateRequests - 1)) + updateTime) / updateRequests;
	}
	
	public static void queryTimings(long start, long stop) {
		queryTime = stop - start;

		totalQueryRequests++;
		
		if (queryMinTime == -1)
			queryMinTime = queryTime;
		else if (queryTime < queryMinTime)
			queryMinTime = queryTime;

		if (queryMaxTime == -1)
			queryMaxTime = queryTime;
		else if (queryTime > queryMaxTime)
			queryMaxTime = queryTime;

		if (queryAverageTime == -1)
			queryAverageTime = queryTime;
		else
			queryAverageTime = ((queryAverageTime * (totalQueryRequests - 1)) + queryTime) / totalQueryRequests;
	}
	
	public static void resetQueryTimings() {
		queryAverageTime = -1;
		queryMaxTime = -1;
		queryMinTime = -1;
		queryTime = -1;
		queryRequests = 0;
		
		updateAverageTime = -1;
		updateMaxTime = -1;
		updateMinTime = -1;
		updateTime = -1;
		updateRequests = 0;
		
		totalQueryRequests = 0;
		totalRequests = 0;	
	}
	
	public static void resetUpdateTimings() {
		updateAverageTime = -1;
		updateMaxTime = -1;
		updateMinTime = -1;
		updateTime = -1;
		updateRequests = 0;
	}

	public static void newRequest(Request request) {
		totalRequests++;
		
		if (request.getClass().equals(QueryRequest.class)) {
			queryRequests++;	
		}
	}
	
	public static String getStatistics() {
		long spus = (totalQueryRequests- queryRequests);
		return String.format("Total requests: %d Query (SPUs) %d (%d) [%.0f %.0f %.0f] Update %d [%.0f %.0f %.0f]",totalRequests, queryRequests,spus,queryMinTime,queryAverageTime,queryMaxTime,updateRequests,updateMinTime,updateAverageTime,updateMaxTime);
	}
	
	public static float getQueryTime_ms() {
		return queryTime;
	}
	
	public static float getUpdateTime_ms() {
		return updateTime;
	}
}
