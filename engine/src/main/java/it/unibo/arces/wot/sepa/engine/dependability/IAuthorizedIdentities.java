package it.unibo.arces.wot.sepa.engine.dependability;

//import java.util.List;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IAuthorizedIdentities {
	public void addAuthorizedIdentity(String id) throws SEPASecurityException;
	public void removeAuthorizedIdentity(String id) throws SEPASecurityException;
	public boolean isAuthorized(String id) throws SEPASecurityException;
}
