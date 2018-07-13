package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ISubscriptionProtocol {

	boolean connect(ISubscriptionHandler handler) throws SEPAProtocolException;

	void close();

	Response subscribe(SubscribeRequest request);

	Response unsubscribe(UnsubscribeRequest request);
	
	boolean isSecure();
}
