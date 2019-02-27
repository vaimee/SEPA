package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

interface ISPU {

    Response init();

    BindingsResults getLastBindings();

    String getSPUID();

    void postProcessing(Response res);
    void preProcessing(InternalUpdateRequest req);
}
