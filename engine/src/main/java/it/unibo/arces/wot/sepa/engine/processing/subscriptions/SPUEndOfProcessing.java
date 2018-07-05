package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.Response;

class SPUEndOfProcessing extends Response {
	private boolean timeout;
	
	public SPUEndOfProcessing(boolean b) {
		timeout = b;
	}
	
	public boolean isTimeout() {
		return timeout;
	}
	
}
