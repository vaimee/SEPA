package it.unibo.arces.wot.sepa.engine.dependability;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface ICredentials {
	public void storeCredentials(String client_id, String client_secret) throws SEPASecurityException;
	public boolean isClientRegistered(String id) throws SEPASecurityException;
	public boolean passwordCheck(String id, String secret) throws SEPASecurityException;
}
