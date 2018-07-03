package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import java.util.concurrent.Semaphore;

public class MockSubscriptionHandler implements ISubscriptionHandler {

    private Response response;
    private Semaphore mutex = new Semaphore(0);

    @Override
    public void onSemanticEvent(Notification notify) {
        response = notify;
        mutex.release();
    }

    @Override
    public void onBrokenConnection() {
        mutex.release();
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        response = errorResponse;
        mutex.release();
    }

    public Response getResponse() throws InterruptedException {
        mutex.acquire();
        return response;
    }
}
