package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;
//import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public interface SubscriptionProtocol extends Closeable {
	public void setHandler(ISubscriptionHandler handler);
	
	public void enableSecurity(SEPASecurityManager sm) throws SEPASecurityException;

//	public void close() throws IOException;

	public void subscribe(SubscribeRequest request) throws SEPAProtocolException;

	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
}
