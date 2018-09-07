package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public abstract class SubscriptionProtocol implements Closeable{
	protected final ISubscriptionHandler handler;
	protected final SEPASecurityManager sm;
	
	public SubscriptionProtocol(ISubscriptionHandler handler,SEPASecurityManager sm) {
		this.handler = handler;
		this.sm = sm;
	}

	public abstract void close();

	public abstract void subscribe(SubscribeRequest request) throws SEPAProtocolException;

	public abstract void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
	
	public final SEPASecurityManager getSecurityManager() {
		return sm;
	}
}
