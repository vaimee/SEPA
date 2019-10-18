package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IAuthorizedIdentities {	
	public void addIdentity(DigitalIdentity identity) throws SEPASecurityException;	
	public void removeIdentity(String uid) throws SEPASecurityException;
	public DigitalIdentity getIdentity (String uid) throws SEPASecurityException;
	public boolean isAuthorized(String identity) throws SEPASecurityException;
}
