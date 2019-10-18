package it.unibo.arces.wot.sepa.engine.dependability.authorization;

public class ApplicationIdentity extends DigitalIdentity{

	public ApplicationIdentity(String uid) {
		super(uid);
	}
	
	public ApplicationIdentity(String uid,Credentials cred) {
		super(uid,cred);
	}

	@Override
	public String getObjectClass() {
		return "applicationProcess";
	}

}
