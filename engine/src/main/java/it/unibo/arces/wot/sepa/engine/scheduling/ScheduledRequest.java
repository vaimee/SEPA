package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

public class ScheduledRequest {
	private Request request = null;
	private ResponseHandler handler = null;
	
	public ScheduledRequest(int token,Request request,ResponseHandler handler) {
		this.request = request;
		this.request.setToken(token);
		this.handler = handler;
	}
	
	public Request getRequest() {
		return request;
	}
	
	public ResponseHandler getHandler() {
		return handler;
	}
	
	public boolean isUpdateRequest() {
		return request.isUpdateRequest();
	}
	
	public boolean isQueryRequest() {
		return request.isQueryRequest();
	}
	
	public boolean isSubscribeRequest() {
		return request.isSubscribeRequest();
	}
	
	public boolean isUnsubscribeRequest() {
		return request.isUnsubscribeRequest();
	}
}
