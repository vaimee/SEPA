package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

public abstract class SubscriptionProtocol {
	protected ISubscriptionHandler handler;
	
	public SubscriptionProtocol(ISubscriptionHandler handler) {
		this.handler = handler;
	}

	public abstract void close();

	public abstract void subscribe(SubscribeRequest request) throws SEPAProtocolException;

	public abstract void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
	
	public abstract boolean isSecure();
}
