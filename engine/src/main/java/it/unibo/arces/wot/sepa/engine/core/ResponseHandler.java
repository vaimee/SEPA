package it.unibo.arces.wot.sepa.engine.core;

import java.util.UUID;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public abstract class ResponseHandler {
	private final UUID uuid;
	
	public ResponseHandler() {
		uuid = UUID.randomUUID();
	}
	
	public abstract void sendResponse(Response response) throws SEPAProtocolException;
	
	public final UUID getUUID() {
		return uuid;
	}
}
