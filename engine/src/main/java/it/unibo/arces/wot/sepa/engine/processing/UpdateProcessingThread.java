package it.unibo.arces.wot.sepa.engine.processing;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerRequestResponseQueue;

public class UpdateProcessingThread extends Thread {
	private UpdateProcessor updateProcessor;
	private SchedulerRequestResponseQueue schedulerQueue;
	private SubscribeProcessor subscribeProcessor;

	private LinkedBlockingQueue<UpdateRequest> updateQueue = new LinkedBlockingQueue<UpdateRequest>();
	
	private final AtomicBoolean end = new AtomicBoolean(false);
	
	public UpdateProcessingThread(UpdateProcessor updateProcessor, SubscribeProcessor subscribeProcessor,
			SchedulerRequestResponseQueue queue) {
		this.updateProcessor = updateProcessor;
		this.schedulerQueue = queue;
		this.subscribeProcessor = subscribeProcessor;
		
		setName("SEPA-Update-Scheduler");
	}

	public void run() {
		while (!end.get()) {
			UpdateRequest request;
			try {
				request = updateQueue.take();
			} catch (InterruptedException e) {
				return;
			}

			// Process update request
			Response ret = updateProcessor.process(request);

			// Notify update result
			synchronized(schedulerQueue) {
				if (schedulerQueue != null) schedulerQueue.addResponse(ret);
			}

			// Subscription processing
			if (ret.isUpdateResponse()) {
				subscribeProcessor.process((UpdateResponse) ret);
			}
		}
	}
	
	public void finish(){
        end.set(true);
    }
	
	public void process(UpdateRequest req) {
		updateQueue.add(req);
	}

	public void setSchedulerQueue(SchedulerRequestResponseQueue queue) {
		synchronized(schedulerQueue) {
			schedulerQueue = queue;
		}
	}
}
