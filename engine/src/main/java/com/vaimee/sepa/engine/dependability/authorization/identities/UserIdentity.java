package com.vaimee.sepa.engine.dependability.authorization.identities;

import com.vaimee.sepa.api.commons.security.Credentials;

public class UserIdentity extends DigitalIdentity {

	private final String commonName;
	private final String surname;

	public UserIdentity(String uid,String cn,String sn) {
		super(uid);
		commonName = cn;
		surname = sn;
	}
	
	public UserIdentity(String uid,String cn,String sn,Credentials cred) {
		super(uid,cred);
		commonName = cn;
		surname = sn;
	}

	@Override
	public String getObjectClass() {
		return "inetOrgPerson";
	}

	public String getCommonName() {
		return commonName;
	}

	public String getSurname() {
		return surname;
	}
	
	public String toString() {
		return "Cn: "+commonName+ " Sn: "+surname+" "+ super.toString();
	}

}
