package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public interface ISubscriptionProcUnit extends Runnable {

    Response init();

    BindingsResults getCurrentResults();

    void terminate();

    String getUUID();

    void process(UpdateResponse res);
}
