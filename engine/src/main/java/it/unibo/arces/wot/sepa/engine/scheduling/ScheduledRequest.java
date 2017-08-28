package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

public class ScheduledRequest {
	private Request request;
	
	private long scheduledTime;
	private long timeout;
	private ResponseHandler handler;
	
	public ScheduledRequest(Integer token,Request request,long timeout,ResponseHandler handler) {
		this.scheduledTime = System.currentTimeMillis();
		this.request = request;
		this.request.setToken(token);
		this.timeout = timeout;
		this.handler = handler;
	}
	
	public long getScheduledTime() {
		return scheduledTime;
	}
	
	public Request getRequest() {
		return request;
	}

	public Integer getToken() {
		return request.getToken();
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public EventHandler getEventHandler(){
		return (EventHandler) handler;
	}
	
	public ResponseHandler getResponseHandler() {
		return handler;
	}
}
