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
		
		/*JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
		
		if (response.getClass().equals(QueryResponse.class))
			HttpUtilities.sendResponse(handler, json.get("code").getAsInt(), json.get("body").toString());
		else
			HttpUtilities.sendResponse(handler, json.get("code").getAsInt(), json.toString());	
		*/
		
		if (response.isError()) {
			ErrorResponse err = (ErrorResponse) response;
			HttpUtilities.sendResponse(handler,err.getErrorCode(),response.toString());
		}
		else
			HttpUtilities.sendResponse(handler, HttpStatus.SC_OK, response.toString());
		
		
		logger.info("Response #"+response.getToken()+" ("+jmx.timings(handler)+" ms)");	
	}

}
