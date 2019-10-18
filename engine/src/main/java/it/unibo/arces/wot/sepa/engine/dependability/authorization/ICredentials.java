package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface ICredentials {
	void storeCredentials(DigitalIdentity identity,String secret) throws SEPASecurityException;
	void removeCredentials(DigitalIdentity identity) throws SEPASecurityException;	
	boolean containsCredentials(String uid) throws SEPASecurityException;
	boolean checkCredentials(String uid, String secret) throws SEPASecurityException;
	Credentials getEndpointCredentials(String uid) throws SEPASecurityException;;
}
