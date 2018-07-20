package it.unibo.arces.wot.sepa.engine.core;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ResponseHandler {
	public void sendResponse(Response response) throws SEPAProtocolException;
}
