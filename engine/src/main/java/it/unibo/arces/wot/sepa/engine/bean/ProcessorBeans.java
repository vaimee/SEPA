package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

public class ProcessorBeans {
	
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
	
	private static String host;
	private static int port;
	private static String queryPath;
	private static String updatePath;
	private static String updateMethod;
	private static String queryMethod;
	private static int updateTimeout;
	private static int queryTimeout;
	
	public static void setEndpoint(SPARQL11Properties prop) {
		host = prop.getDefaultHost();
		port = prop.getDefaultPort();
		queryPath = prop.getDefaultQueryPath();
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

		queryRequests++;
		
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
			queryAverageTime = ((queryAverageTime * (queryRequests - 1)) + queryTime) / queryRequests;
	}
	
	public static void reset() {
		 updateRequests = 0;
		 queryRequests = 0 ;
		 
		 queryMinTime = -1;
		 queryAverageTime = -1;
		 queryMaxTime = -1;
		 queryTime = -1;
		 	
		 updateMinTime = -1;
		 updateAverageTime = -1;
		 updateMaxTime = -1;
		 updateTime = -1;
	}
	
	public static float getQueryTime_ms() {
		return queryTime;
	}
	
	public static float getUpdateTime_ms() {
		return updateTime;
	}

	public static long getProcessedRequests() {
		return updateRequests+queryRequests;
	}

	public static long getProcessedQueryRequests() {
		return queryRequests;
	}

	public static long getProcessedUpdateRequests() {
		return updateRequests;
	}

	public static float getTimings_QueryTime_Max_ms() {
		return queryMaxTime;
	}

	public static float getTimings_UpdateTime_Min_ms() {
		return updateMinTime;
	}

	public static float getTimings_UpdateTime_Average_ms() {
		return updateAverageTime;
	}

	public static float getTimings_UpdateTime_Max_ms() {
		return updateMaxTime;
	}

	public static float getTimings_QueryTime_Min_ms() {
		return queryMinTime;
	}

	public static float getTimings_QueryTime_Average_ms() {
		return queryAverageTime;
	}

	public static int getUpdateTimeout() {
		return updateTimeout;
	}

	public static int getQueryTimeout() {
		return queryTimeout;
	}

	public static void setUpdateTimeout(int t) {
		updateTimeout = t;
	}

	public static void setQueryTimeout(int t) {
		queryTimeout = t;
	}
}
