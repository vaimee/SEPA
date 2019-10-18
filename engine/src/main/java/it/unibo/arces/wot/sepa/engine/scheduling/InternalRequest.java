package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public abstract class InternalRequest {
	private Credentials credentials;
	
	public InternalRequest(Credentials credentials) {
		this.credentials = credentials;
	}
	
	public boolean isQueryRequest() {
		return this.getClass().equals(InternalQueryRequest.class);
	}

	public boolean isUpdateRequest() {
		return this.getClass().equals(InternalUpdateRequest.class);
	}

	public boolean isSubscribeRequest() {
		return this.getClass().equals(InternalSubscribeRequest.class);
	}

	public boolean isUnsubscribeRequest() {
		return this.getClass().equals(InternalUnsubscribeRequest.class);
	}
	
	public Credentials getCredentials() {
		return credentials;
	}
}
