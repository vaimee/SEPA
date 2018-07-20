package it.unibo.arces.wot.sepa.engine.scheduling;

public abstract class InternalRequest {

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
}
