package it.unibo.arces.wot.sepa.commons.protocol;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

public interface ISPARQL11SEProtocol extends ISPARQL11Interface {
	
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException;
	
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
	
}
