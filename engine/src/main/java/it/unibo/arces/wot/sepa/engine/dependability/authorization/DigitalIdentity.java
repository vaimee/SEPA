package it.unibo.arces.wot.sepa.engine.dependability.authorization;

public abstract class DigitalIdentity {
	
	private String uid;
	private Credentials endpointCredentials = new Credentials("SEPATest","SEPATest");
	
	public DigitalIdentity(String uid) {
		this.uid = uid;
	}
	
	public DigitalIdentity(String uid,Credentials cred) {
		this.uid = uid;
		if (cred != null) this.endpointCredentials = cred;
	}
	
	public String getUid() {
		return uid;
	}
	
	public abstract String getObjectClass();
	
	public Credentials getEndpointCredentials() {
		return endpointCredentials;
	}
}
