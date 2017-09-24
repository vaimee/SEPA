package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import org.apache.http.nio.protocol.HttpAsyncExchange;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;

public class SPARQL11ResponseHandler implements ResponseHandler {

	HttpAsyncExchange handler;
	
	public SPARQL11ResponseHandler(HttpAsyncExchange handler) {
		this.handler = handler;
	}
	
	@Override
	public void sendResponse(Response response) {
		JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();
		
		if (response.getClass().equals(QueryResponse.class))
			HttpUtilities.sendResponse(handler, json.get("code").getAsInt(), json.get("body").toString());
		else
			HttpUtilities.sendResponse(handler, json.get("code").getAsInt(), json.toString());	
		
	}

}
