package it.unibo.arces.wot.sepa.engine.scheduling;

public class InternalUnsubscribeRequest extends InternalRequest {
	protected String spuid;
	
	public InternalUnsubscribeRequest(String spuid) {
		this.spuid = spuid;
	}
	
	@Override
	public String toString() {
		return "*UNSUBSCRIBE* "+spuid;
	}
	
	public String getSpuid() {
		return spuid;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalUnsubscribeRequest)) return false;
		return spuid.equals(((InternalUnsubscribeRequest)obj).spuid);
	}
}
