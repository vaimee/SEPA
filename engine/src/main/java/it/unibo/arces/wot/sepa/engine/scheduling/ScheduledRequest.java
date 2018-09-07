package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

public class ScheduledRequest {
	private InternalRequest request = null;
	private ResponseHandler handler = null;
	private int token;
	
	public ScheduledRequest(int token,InternalRequest request,ResponseHandler handler) {
		this.request = request;
		this.handler = handler;
		this.token = token;
	}
	
	@Override
	public String toString() {
		return "REQUEST #"+token+" : "+request.toString();
	}
	
	public int getToken() {
		return token;
	}
	
	public InternalRequest getRequest() {
		return request;
	}
	
	public ResponseHandler getResponseHandler() {
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
