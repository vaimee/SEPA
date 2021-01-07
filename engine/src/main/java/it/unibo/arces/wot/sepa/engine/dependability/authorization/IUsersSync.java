package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public interface IUsersSync {
	public JsonObject sync() throws SEPASecurityException;
	public String getEndpointUsersPassword();
}
