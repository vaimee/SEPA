package it.unibo.arces.wot.sepa.engine.core;

import java.util.concurrent.ConcurrentLinkedQueue;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

public class SchedulerRequestResponseQueue {
	private ConcurrentLinkedQueue<ScheduledRequest> requests = new ConcurrentLinkedQueue<ScheduledRequest>();
	private ConcurrentLinkedQueue<Response> responses = new ConcurrentLinkedQueue<Response>();
	
	public void addRequest(ScheduledRequest req) {
		requests.add(req);
		synchronized(requests) {
			requests.notify();
		}
	}
	
	public ScheduledRequest waitRequest() throws InterruptedException {
		ScheduledRequest req;
		while((req =requests.poll()) == null) {
			synchronized(requests) {
				requests.wait();
			}
		}
		return req;
	}
	
	public void addResponse(Response res) {
		responses.add(res);
		synchronized(responses) {
			responses.notify();
		}
	}
	
	public Response waitResponse() throws InterruptedException {
		Response res;
		while((res = responses.poll()) == null) {
			synchronized(responses) {
				responses.wait();
			}
		}
		return res;
	}
}
