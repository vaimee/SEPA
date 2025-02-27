package com.vaimee.sepa.engine.dependability.authorization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vaimee.sepa.commons.exceptions.SEPASecurityException;

public interface IUsersAcl {
	public void createUser(String uid, JsonElement graphs) throws SEPASecurityException;
	public void removeUser(String uid) throws SEPASecurityException;
	public void updateUser(String uid, JsonObject addGraphs, JsonArray removeGraphs) throws SEPASecurityException;
}
