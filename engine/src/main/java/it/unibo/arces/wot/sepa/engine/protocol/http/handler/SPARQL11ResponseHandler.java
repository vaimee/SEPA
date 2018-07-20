package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.timing.Timings;

public class SPARQL11ResponseHandler implements ResponseHandler {
	protected final Logger logger = LogManager.getLogger();
	
	private HttpAsyncExchange handler;
	private HTTPHandlerBeans jmx;
	
	public SPARQL11ResponseHandler(HttpAsyncExchange httpExchange, HTTPHandlerBeans jmx) {
		this.handler = httpExchange;
		this.jmx = jmx;
		jmx.start(handler);
	}

	@Override
	public void sendResponse(Response response) {
		if (response.isError()) {
			ErrorResponse err = (ErrorResponse) response;
			HttpUtilities.sendResponse(handler,err.getErrorCode(),response.toString());
		}
		else
			HttpUtilities.sendResponse(handler, HttpStatus.SC_OK, response.toString());
		
		Timings.log(response);
		jmx.stop(handler);
		logger.trace(response);	
	}

}
