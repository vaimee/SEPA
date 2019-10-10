package it.unibo.arces.wot.sepa.engine.dependability;

public class AuthorizationResponse {

	private boolean authorized = true;
	
	private String error = null;
	private String description = null;
	
	private ClientCredentials credentials = null;
	
	public AuthorizationResponse() {
	}
	
	public AuthorizationResponse(String error,String description) {
		this.authorized = false;
		this.error = error;
		this.description = description;
	}
	
	public AuthorizationResponse(ClientCredentials credentials) {
		this.credentials = credentials;
	}
	
	public boolean isAuthorized() {
		return authorized;
	}
	
	public ClientCredentials getClientCredentials() {
		return credentials;
	}
	
	public String getError() {
		return error;
	}
	
	public String getDescription() {
		return description;
	}

}
