package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.request.Request;

public class ScheduledRequest {
	private Request request;
	private ResponseAndNotificationListener listener;
	private long scheduledTime;
	
	public ScheduledRequest(Integer token,Request request,ResponseAndNotificationListener listener) {
		scheduledTime = System.currentTimeMillis();
		this.request = request;
		this.listener = listener;
		request.setToken(token);
	}
	
	public long getScheduledTime() {
		return scheduledTime;
	}
	
	public ResponseAndNotificationListener getListener(){
		return listener;
	}
	
	public Request getRequest() {
		return request;
	}

	public Integer getToken() {
		return request.getToken();
	}
}
