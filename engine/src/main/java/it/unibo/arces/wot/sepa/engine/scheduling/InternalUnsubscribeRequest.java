package it.unibo.arces.wot.sepa.engine.scheduling;

public class InternalUnsubscribeRequest extends InternalRequest {
	protected String sid;
	protected String gid;
	
	public InternalUnsubscribeRequest(String gid,String sid) {
		this.sid = sid;
		this.gid = gid;
	}
	
	@Override
	public String toString() {
		return "*UNSUBSCRIBE* "+sid;
	}
	
	public String getSID() {
		return sid;
	}
	
	public String getGID() {
		return gid;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof InternalUnsubscribeRequest)) return false;
		return sid.equals(((InternalUnsubscribeRequest)obj).sid);
	}
}
