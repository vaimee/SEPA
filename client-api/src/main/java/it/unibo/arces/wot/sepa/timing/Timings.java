package it.unibo.arces.wot.sepa.timing;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class Timings {
	private static final Logger logger = LogManager.getLogger();
	
	public static long getTime() {
		return System.nanoTime();
	}
	
	public synchronized static void log(String tag,long start,long stop) {
		String message = String.format("%d,%s,%d",System.currentTimeMillis(),tag,stop-start);
		logger.log(Level.getLevel("timing"),message);
	}
	
	public synchronized static void log(Request request) {
		long start = getTime();
		
		String tag;
		if (request.isUpdateRequest()) tag = "REQUEST_UPDATE_";
		else if (request.isSubscribeRequest()) tag = "REQUEST_SUBSCRIBE_";
		else if(request.isQueryRequest()) tag = "REQUEST_QUERY_"; 
		else if(request.isUnsubscribeRequest()) tag = "REQUEST_UNSUBSCRIBE_";
		else tag = "REQUEST_UNKNOWN_";
		
		log(tag+request.getToken(),start,start);
	}
	
	public synchronized static void log(Response response) {
		long start = getTime();
		
		String tag;
		if (response.isUpdateResponse()) tag = "RESPONSE_UPDATE_";
		else if (response.isSubscribeResponse()) tag = "RESPONSE_SUBSCRIBE_";
		else if(response.isQueryResponse()) tag = "RESPONSE_QUERY_"; 
		else if(response.isUnsubscribeResponse()) tag = "RESPONSE_UNSUBSCRIBE_"; 
		else if(response.isError()) tag = "RESPONSE_ERROR_";
		else tag = "RESPONSE_UNKNOWN_";
		
		log(tag+response.getToken(),start,start);
	}
}
