package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.concurrent.LinkedBlockingQueue;

import it.unibo.arces.wot.sepa.commons.response.Response;

public class SchedulerRequestResponseQueue {
	private LinkedBlockingQueue<ScheduledRequest> requests = new LinkedBlockingQueue<ScheduledRequest>();
	private LinkedBlockingQueue<Response> responses = new LinkedBlockingQueue<Response>();
	
	public void addRequest(ScheduledRequest req) {
		requests.offer(req);
	}
	
	public ScheduledRequest waitRequest() throws InterruptedException {
		return requests.take();
	}
	
	public void addResponse(Response res) {
		responses.offer(res);
	}
	
	public Response waitResponse() throws InterruptedException {
		return responses.take();
	}
}
