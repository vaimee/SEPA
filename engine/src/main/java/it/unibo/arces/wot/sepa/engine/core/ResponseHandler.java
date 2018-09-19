package it.unibo.arces.wot.sepa.engine.core;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ResponseHandler {	
	public abstract void sendResponse(Response response) throws SEPAProtocolException;
}
