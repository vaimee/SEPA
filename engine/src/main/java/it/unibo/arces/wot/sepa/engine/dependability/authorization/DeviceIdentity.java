package it.unibo.arces.wot.sepa.engine.dependability.authorization;

public class DeviceIdentity extends DigitalIdentity {

	public DeviceIdentity(String uid) {
		super(uid);
	}

	public DeviceIdentity(String uid,Credentials cred) {
		super(uid,cred);
	}
	
	@Override
	public String getObjectClass() {
		return "device";
	}

}
