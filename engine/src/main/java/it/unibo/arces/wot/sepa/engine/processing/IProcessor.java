package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

public interface IProcessor {
	
	boolean isRunning();
	void start();
	void interrupt();
	Response processSubscribe(InternalSubscribeRequest request);
	Response processUnsubscribe(String sid, String gid);
	Response processUpdate(InternalUpdateRequest update);
	Response processQuery(InternalQueryRequest query) throws SEPASecurityException, IOException;
	boolean isUpdateReliable();
	ScheduledRequest waitQueryRequest() throws InterruptedException;
	ScheduledRequest waitSubscribeRequest() throws InterruptedException;
	ScheduledRequest waitUpdateRequest() throws InterruptedException;
	ScheduledRequest waitUnsubscribeRequest() throws InterruptedException;
	public void addResponse(int token, Response ret);
	
}
