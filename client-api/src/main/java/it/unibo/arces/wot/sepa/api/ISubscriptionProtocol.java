package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public interface ISubscriptionProtocol {

	void setHandler(ISubscriptionHandler handler) throws SEPAProtocolException;

	void close();

	Response subscribe(String sparql);

	Response secureSubscribe(String sparql, String authorization);

	Response unsubscribe(String subscribeUUID);

	Response secureUnsubscribe(String subscribeUUID, String authorization);

}
