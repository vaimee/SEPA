package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class LdapProperties {
	private final String host;
	private final int port;
	private final boolean tls;
	private final String base;
	private final String user;
	private final String pass;
	private final String usersDNsuffix;
	
	public LdapProperties(String host, int port, String base, String usersDNsuffix, String user, String pass,boolean tls) throws SEPASecurityException {
		if (host == null)  throw new SEPASecurityException("Host is null");
		if (base == null)  throw new SEPASecurityException("Base is null");
		if (user != null && pass == null)  throw new SEPASecurityException("Password is null");
		
		this.host = host;
		this.pass = pass;
		this.port = port;
		this.user = user;
		this.tls = tls;
		this.base = base;
		this.usersDNsuffix = usersDNsuffix;
	}
	
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
	public boolean isTls() {
		return tls;
	}
	public String getBase() {
		return base;
	}
	
	public String getUsersDN() {
		return usersDNsuffix;
	}
	
	public String getUser() {
		return user;
	}
	public String getPass() {
		return pass;
	}
}
