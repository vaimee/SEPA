package it.unibo.arces.wot.sepa.engine.dependability;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;

public class AuthorizationResponse {

	private boolean authorized = true;
	
	private String error = null;
	private String description = null;
	
	private Credentials credentials = null;
	
	public AuthorizationResponse() {
	}
	
	public AuthorizationResponse(String error,String description) {
		this.authorized = false;
		this.error = error;
		this.description = description;
	}
	
	public AuthorizationResponse(Credentials credentials) {
		this.credentials = credentials;
	}
	
	public boolean isAuthorized() {
		return authorized;
	}
	
	public Credentials getClientCredentials() {
		return credentials;
	}
	
	public String getError() {
		return error;
	}
	
	public String getDescription() {
		return description;
	}

}
