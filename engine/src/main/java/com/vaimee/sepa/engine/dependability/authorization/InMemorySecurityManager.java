package com.vaimee.sepa.engine.dependability.authorization;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import javax.net.ssl.SSLContext;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.security.Credentials;
import com.vaimee.sepa.engine.dependability.authorization.identities.ApplicationIdentity;
import com.vaimee.sepa.engine.dependability.authorization.identities.DeviceIdentity;
import com.vaimee.sepa.engine.dependability.authorization.identities.DigitalIdentity;
import com.vaimee.sepa.logging.Logging;

public class InMemorySecurityManager extends SecurityManager {
	public InMemorySecurityManager(SSLContext ssl,RSAKey key)
			throws SEPASecurityException {
		super(ssl, key,true);
		
		// TODO: WARNING!!! JUST FOR TESTING
		addAuthorizedIdentity(new ApplicationIdentity("SEPATest",new Credentials("SEPATest","SEPATest")));
	}
	
	class AuthorizedIdentity {
		DigitalIdentity identity = null;
		SignedJWT token = null;
		Long expiring = null;
		boolean authorized = true;
		
		public AuthorizedIdentity(DigitalIdentity identity) {
			this.identity = identity;
			
			// TODO: WARNING!!! JUST FOR TESTING
			if (identity.getUid().equals("SEPATest")) {
				expiring= SEPATestExpiringTime;
			}
			else if (identity.getClass().equals(DeviceIdentity.class)) {
				expiring = deviceExpiringTime;
			} else if (identity.getClass().equals(ApplicationIdentity.class)) { 
				expiring = applicationExpiringTime;
			} else 
				expiring = defaultExpiringTime;
		}
		
		public void register(String user,String secret) {
			identity.setEndpointCredentials(user, secret);
		}
		
		public void unregister() {
			identity.resetEndpointCredentials();
		}

		public boolean isRegistered() {
			return identity.getEndpointCredentials() != null;
		}

		public boolean checkPassword(String pwd) {
			return (isRegistered() ? identity.getEndpointCredentials().password().equals(pwd) : false);
		}
		
		public boolean containsToken() {
			return token != null;
		}
		
		public void removeToken() {
			token = null;
		}

		public Date getTokenExpiringDate() throws SEPASecurityException {
			JWTClaimsSet claims = null;
			try {
				claims = token.getJWTClaimsSet();
			} catch (ParseException e) {
				throw new SEPASecurityException(e);
			}
			
			return claims.getExpirationTime();
		}

		public void addToken(SignedJWT jwt) throws SEPASecurityException {
			token = jwt;
			
			JWTClaimsSet claims = null;
			try {
				claims = jwt.getJWTClaimsSet();
			} catch (ParseException e) {
				throw new SEPASecurityException(e);
			}
			
			expiring = (claims.getExpirationTime().getTime() - claims.getIssueTime().getTime())/1000;
		}

		public long getExpiringPeriod() {
			return expiring;
		}

		public SignedJWT getToken() {
			return token;
		}

		public DigitalIdentity getIdentity() {
			return identity;
		}

		public void setExpiringPeriod(long period) {
			expiring = period;	
		}

		public boolean isAuthorized() {
			return authorized;
		}

		public void removeIdentity() {
			authorized = false;
		}
	}
	
	private final HashMap<String,AuthorizedIdentity> identities = new HashMap<String,AuthorizedIdentity>();

	private String issuer = "https://localhost:8443/oauth/token";

	private long deviceExpiringTime = 3600;
	private long applicationExpiringTime = 43200;
	private long userExpiringTime = 300;
	private long defaultExpiringTime = 5;
	private long SEPATestExpiringTime = 5;
	
	@Override
	public boolean isAuthorized(String uid) throws SEPASecurityException {
		Logging.debug("isAuthorized "+uid);
		
		if (identities.containsKey(uid)) return identities.get(uid).isAuthorized();
		
		return false;
	}
	
	@Override
	public void addAuthorizedIdentity(DigitalIdentity identity) {
		Logging.debug("addIdentity "+identity.getUid());
		
		identities.put(identity.getUid(),new AuthorizedIdentity(identity));
	}
	@Override
	public void removeAuthorizedIdentity(String uid) {
		Logging.debug("removeIdentity "+uid);
		
		if (uid.equals("SEPATest")) return;
		
		if (identities.containsKey(uid)) identities.get(uid).removeIdentity();
	}
	
	// Client credentials
	@Override
	public boolean storeCredentials(DigitalIdentity identity, String client_secret) {
		Logging.debug("storeCredentials "+identity.getUid()+" : "+client_secret);
		
		identities.put(identity.getUid(), new AuthorizedIdentity(identity));
		identities.get(identity.getUid()).register(identity.getUid(),client_secret);
		
		return true;
	}
	
	@Override
	public void removeCredentials(DigitalIdentity identity) throws SEPASecurityException {
		Logging.debug("removeCredentials "+identity.getUid());
		
		if (identities.containsKey(identity.getUid())) identities.get(identity.getUid()).unregister();
	}
	@Override
	public boolean containsCredentials(String id) {
		Logging.debug("containsCredentials "+id);
		
		if (identities.containsKey(id)) return identities.get(id).isRegistered();
		
		return false;
	}
	
	@Override
	public boolean checkCredentials(String id, String secret) {
		Logging.debug("checkCredentials "+id+" : "+secret);
		
		if (identities.containsKey(id)) return identities.get(id).checkPassword(secret);
		
		return false;
	}
	
	// Client claims
	@Override
	public boolean containsJwt(String id) {
		Logging.debug("containsToken "+id);
		
		if (identities.containsKey(id)) return identities.get(id).containsToken();
		
		return false;
	}
	
	@Override
	public Date getTokenExpiringDate(String id) throws SEPASecurityException {
		Logging.debug("getTokenExpiringDate "+id);
		
		if (identities.containsKey(id)) return identities.get(id).getTokenExpiringDate();
		
		return null;
	}
	
	@Override
	public void addJwt(String id, SignedJWT jwt) throws SEPASecurityException {
		Logging.debug("addToken "+id+" "+jwt.serialize());
		
		if (identities.containsKey(id)) identities.get(id).addToken(jwt);
	}
	
	@Override
	public void removeJwt(String id) throws SEPASecurityException {
		Logging.debug("removeToken "+id);
		
		if (identities.containsKey(id)) identities.get(id).removeToken();
	}

	// JWT
	@Override
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException {
		Logging.debug("getTokenExpiringPeriod "+id);
		
		if (identities.containsKey(id)) return identities.get(id).getExpiringPeriod();
		
		return -1;
	}
	
	@Override
	public void setTokenExpiringPeriod(String id, long period) {
		Logging.debug("setTokenExpiringPeriod "+id+" : "+period);
		
		if (identities.containsKey(id)) identities.get(id).setExpiringPeriod(period);
	}
	
	@Override
	public SignedJWT getJwt(String id) {
		Logging.debug("getToken "+id);
		
		if (identities.containsKey(id)) return identities.get(id).getToken();
		
		return null;
	}
	@Override
	public DigitalIdentity getIdentity(String uid) throws SEPASecurityException {
		Logging.debug("getIdentity "+uid);
		
		if (identities.containsKey(uid)) return identities.get(uid).getIdentity();
		
		return null;
	}
	
	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		Logging.debug("getEndpointCredentials "+uid);
		
		if (identities.containsKey(uid)) return identities.get(uid).getIdentity().getEndpointCredentials();	
		
		return null;
	}
	
	@Override
	public String getIssuer() {
		return issuer;
	}
	@Override
	public void setIssuer(String is) {
		issuer = is;
	}

	@Override
	public void setDeviceExpiringPeriod(long period) throws SEPASecurityException {
		deviceExpiringTime = period;
	}
	@Override
	public long getDeviceExpiringPeriod() throws SEPASecurityException {
		return deviceExpiringTime;
	}
	@Override
	public void setApplicationExpiringPeriod(long period) throws SEPASecurityException {
		applicationExpiringTime = period;
	}
	@Override
	public long getApplicationExpiringPeriod() throws SEPASecurityException {
		return applicationExpiringTime;
	}
	@Override
	public void setUserExpiringPeriod(long period) throws SEPASecurityException {
		userExpiringTime = period;
	}
	@Override
	public long getUserExpiringPeriod() throws SEPASecurityException {
		return userExpiringTime;
	}
	@Override
	public void setDefaultExpiringPeriod(long period) throws SEPASecurityException {
		defaultExpiringTime = period;
	}
	@Override
	public long getDefaultExpiringPeriod() throws SEPASecurityException {
		return defaultExpiringTime;
	}

	@Override
	public boolean isForTesting(String identity) throws SEPASecurityException {
		return identity.equals("SEPATest");
	}
}
