package it.unibo.arces.wot.sepa.engine.core;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ResponseHandler {
	public void sendResponse(Response response) throws IOException;
}
