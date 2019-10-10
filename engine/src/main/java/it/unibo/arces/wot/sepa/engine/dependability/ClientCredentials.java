package it.unibo.arces.wot.sepa.engine.dependability;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class ClientCredentials {
	private String user;
	private String password;

	public ClientCredentials(String user, String password) {
		if (user == null || password == null)
			throw new IllegalArgumentException("User or password are null");
		this.user = user;
		this.password = password;
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public String getBasicAuthorizationHeader() throws SEPASecurityException {
		String plainString = user + ":" + password;
		try {
			return "Basic " + new String(Base64.getEncoder().encode(plainString.getBytes("UTF-8")), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new SEPASecurityException(e);
		}
	}
}
