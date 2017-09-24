package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.Response;

public class SPUManagerNotify extends Response {
	private boolean endOfProcessing;
	private Response response;
	
	public SPUManagerNotify(Response response) {
		endOfProcessing = false;
		this.response = response;
	}
	
	public SPUManagerNotify() {
		endOfProcessing = true;	
		this.response = null;
	}
	
	public boolean isEndOfProcessing() {
		return endOfProcessing;
	}
	
	public Response getResponse() {
		return response;
	}
}
