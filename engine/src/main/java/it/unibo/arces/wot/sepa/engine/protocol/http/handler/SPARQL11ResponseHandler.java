package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.time.Instant;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
//import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.dependability.Timing;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;

public class SPARQL11ResponseHandler implements ResponseHandler {
	protected final Logger logger = LogManager.getLogger("SPARQL11ResponseHandler");
	
	private HttpAsyncExchange handler;
	private HTTPHandlerBeans jmx;
	
	public SPARQL11ResponseHandler(HttpAsyncExchange httpExchange, HTTPHandlerBeans jmx, Instant start) {
		this.handler = httpExchange;
		this.jmx = jmx;
		jmx.newRequest(handler,start);
	}

	@Override
	public void sendResponse(Response response) {
		Timing.logTiming(response, "RESPONDING", Instant.now());
		
		if (response.isError()) {
			ErrorResponse err = (ErrorResponse) response;
			HttpUtilities.sendResponse(handler,err.getErrorCode(),response.toString());
		}
		else
			HttpUtilities.sendResponse(handler, HttpStatus.SC_OK, response.toString());
		
		Timing.logTiming(response, "RESPONSE_SENT", Instant.now());
		logger.info("Response #"+response.getToken()+" ("+jmx.timings(handler)+" ms)");	
	}

}
