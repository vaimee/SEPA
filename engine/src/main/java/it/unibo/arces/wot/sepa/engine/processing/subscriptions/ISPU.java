package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

interface ISPU {

    Response init();

    BindingsResults getLastBindings();

    String getUUID();

    void process(UpdateResponse res);
}
