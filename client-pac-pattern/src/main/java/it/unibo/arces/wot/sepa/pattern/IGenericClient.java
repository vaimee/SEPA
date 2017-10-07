package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public interface IGenericClient {
	Response update(String SPARQL_UPDATE,Bindings forced);
	Response query(String SPARQL_QUERY,Bindings forced);
	Response subscribe(String SPARQL_SUBSCRIBE,Bindings forced);
	Response unsubscribe(String subID);
	
	void onResults(ARBindingsResults results);
	void onAddedResults(BindingsResults results);
	void onRemovedResults(BindingsResults results);
	
	void onKeepAlive();
	void onBrokenSubscription();
	void onSubscriptionError(ErrorResponse errorResponse);
}
