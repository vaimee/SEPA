package it.unibo.arces.wot.sepa.engine.scheduling;

import it.unibo.arces.wot.sepa.engine.dependability.ClientCredentials;

public abstract class InternalRequest {
	private ClientCredentials credentials;
	
	public InternalRequest(ClientCredentials credentials) {
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
	
	public ClientCredentials getCredentials() {
		return credentials;
	}
}
