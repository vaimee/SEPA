package it.unibo.arces.wot.sepa.engine.dependability;

import java.time.Instant;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class Timing {
	private static final Logger logger = LogManager.getLogger("");
	
	public static void logTiming(Request request,String TAG,Instant instant) {
		String type = " ";
		
		if(request.isQueryRequest()) type = "QUERY ";
		else if (request.isUpdateRequest()) type = "UPDATE ";
		else if (request.isSubscribeRequest()) type = "SUBSCRIBE ";
		else type = "UNSUBSCRIBE ";
		
		logger.log(Level.getLevel("timing"), type + request.getToken()+ " "+TAG+" "+instant.toEpochMilli());
	}
	
	public static void logTiming(Response response,String TAG,Instant instant) {
		String type = " ";
		
		if(response.isQueryResponse()) type = "QUERY ";
		else if (response.isUpdateResponse()) type = "UPDATE ";
		else if (response.isSubscribeResponse()) type = "SUBSCRIBE ";
		else if (response.isUnsubscribeResponse()) type =  "UNSUBSCRIBE ";
		else type = "ERROR ";
		
		logger.log(Level.getLevel("timing"), type + response.getToken()+ " "+TAG+" "+instant.toEpochMilli());
	}
}
