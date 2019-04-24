package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

interface ISPU {
	String getSPUID();
	
    Response init();

    BindingsResults getLastBindings();

    void postUpdateProcessing(Response res);
    void preUpdateProcessing(InternalUpdateRequest req);
}
