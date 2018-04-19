package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import java.util.concurrent.Semaphore;

public class MockSubscriptionHandler implements ISubscriptionHandler {

    private Response response;
    private boolean ping = false;
    private Semaphore pingSemaphore = new Semaphore(0);
    private Semaphore notSemaphore = new Semaphore(0);

    @Override
    public void onSemanticEvent(Notification notify) {
        this.response = notify;
        notSemaphore.release();
    }

    @Override
    public void onPing() {
        ping = true;
        pingSemaphore.release();
    }

    @Override
    public void onBrokenSocket() {
        pingSemaphore.release();
        notSemaphore.release();
    }

    @Override
    public void onError(ErrorResponse errorResponse) {
        pingSemaphore.release();
        response = errorResponse;
        notSemaphore.release();
    }

    public boolean pingRecived() throws InterruptedException {
        pingSemaphore.acquire();
        return ping;
    }

    public Response getResponse() throws InterruptedException {
        notSemaphore.acquire();
        return response;
    }
}
