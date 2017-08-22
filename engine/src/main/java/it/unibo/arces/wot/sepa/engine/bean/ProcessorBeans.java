package it.unibo.arces.wot.sepa.engine.bean;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;

public class ProcessorBeans {
	private static SPARQL11Properties endpointProperties;
	
	private static int totalRequests = 0;
	private static int subscribeRequests = 0;
	private static int unsubscribeRequests = 0;
	private static int unsupportedRequests = 0;
	//private static int totalUpdateRequests = 0;
	private static int totalQueryRequests = 0;
	
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
	
	public static void setEndpoint(SPARQL11Properties prop) {
		endpointProperties = prop;	
	}
	
	public static String getEndpointProperties() {
		return endpointProperties.toString();	
	}
	
	public static String getRequests() {
		long spus = (totalQueryRequests- queryRequests);
		return String.format("%d [Updates: %d Queries (SPUs): %d Queries: %d Subscribes: %d Unsubscribes: %d Unsupported: %d]", totalRequests,updateRequests,spus,queryRequests,subscribeRequests,unsubscribeRequests,unsupportedRequests);
	}
	
 	public static void updateTimings(long start, long stop) {
		updateTime = stop - start;

		//totalUpdateRequests++;
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
		//queryRequests++;
		
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
		//totalUpdateRequests = 0;
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
		
		if (request.getClass().equals(UpdateRequest.class)) {
			
		} 
		else if (request.getClass().equals(QueryRequest.class)) {
			queryRequests++;	
		}
		else if (request.getClass().equals(SubscribeRequest.class)) {
			subscribeRequests++;
		}  
		else if (request.getClass().equals(UnsubscribeRequest.class)) {
			unsubscribeRequests++;
		} 
		else {
			unsupportedRequests++;
		}
		
	}

	public static String getQueryTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", queryTime,queryMinTime,queryAverageTime,queryMaxTime);
	}

	public static String getUpdateTimings() {
		return String.format("%.0f ms [Min: %.0f Avg: %.0f Max: %.0f]", updateTime,updateMinTime,updateAverageTime,updateMaxTime);
	}
}
