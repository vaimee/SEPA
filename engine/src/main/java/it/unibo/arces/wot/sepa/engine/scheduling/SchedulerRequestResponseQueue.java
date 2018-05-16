package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.concurrent.LinkedBlockingQueue;

import it.unibo.arces.wot.sepa.commons.response.Response;

public class SchedulerRequestResponseQueue {
	private LinkedBlockingQueue<ScheduledRequest> requests = new LinkedBlockingQueue<ScheduledRequest>();
	private LinkedBlockingQueue<Response> responses = new LinkedBlockingQueue<Response>();
	
	public void addRequest(ScheduledRequest req) {
		try {
			requests.put(req);
		} catch (InterruptedException e) {
			return;
		}
	}
	
	public ScheduledRequest waitRequest() throws InterruptedException {
		return requests.take();
	}
	
	public void addResponse(Response res) {
		try {
			responses.put(res);
		} catch (InterruptedException e) {
			return;
		}
	}
	
	public Response waitResponse() throws InterruptedException {
		return responses.take();
	}
}
