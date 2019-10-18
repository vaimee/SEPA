package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class InMemoryAuthorization implements IAuthorization {
	private static final Logger logger = LogManager.getLogger();
	
	class AuthorizedIdentity {
		DigitalIdentity identity = null;
		String secret = null;
		String user = null;
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
			this.secret = secret;
			this.user = user;
		}
		
		public void unregister() {
			this.secret = null;
			this.user = null;
		}

		public boolean isRegistered() {
			return (user != null && secret != null);
		}

		public boolean checkPassword(String pwd) {
			return pwd.equals(secret);
		}
		
		public boolean containsToken() {
			return token != null;
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

	private String issuer = "https://wot.arces.unibo.it:8443/oauth/token";
	private String httpsAudience = "https://wot.arces.unibo.it:8443/sparql";
	private String wssAudience = "wss://wot.arces.unibo.it:9443/sparql";
	private String subject = "SEPATest";

	private long deviceExpiringTime = 3600;
	private long applicationExpiringTime = 43200;
	private long userExpiringTime = 300;
	private long defaultExpiringTime = 5;
	private long SEPATestExpiringTime = 5;
	
	public InMemoryAuthorization() {
		// TODO: WARNING!!! JUST FOR TESTING
		addIdentity(new ApplicationIdentity("SEPATest",new Credentials("SEPATest","SEPATest")));
	}
	
	@Override
	public boolean isAuthorized(String uid) throws SEPASecurityException {
		logger.debug("isAuthorized "+uid);
		
		if (identities.get(uid) == null) return false;
		
		return identities.get(uid).isAuthorized();
	}
	
	@Override
	public void addIdentity(DigitalIdentity identity) {
		logger.debug("addIdentity "+identity.getUid());
		identities.put(identity.getUid(),new AuthorizedIdentity(identity));
	}
	@Override
	public void removeIdentity(String uid) {
		logger.debug("removeIdentity "+uid);
		
		if (uid.equals("SEPATest")) return;
		
		if (identities.get(uid) != null) identities.get(uid).removeIdentity();
	}
	
	// Client credentials
	@Override
	public void storeCredentials(DigitalIdentity identity, String client_secret) {
		logger.debug("storeCredentials "+identity.getUid()+" : "+client_secret);
		identities.get(identity.getUid()).register(identity.getUid(),client_secret);
	}
	
	@Override
	public void removeCredentials(DigitalIdentity identity) throws SEPASecurityException {
		logger.debug("removeCredentials "+identity.getUid());
		identities.get(identity.getUid()).unregister();
	}
	@Override
	public boolean containsCredentials(String id) {
		logger.debug("containsCredentials "+id);
		return identities.get(id).isRegistered();
	}
	
	@Override
	public boolean checkCredentials(String id, String secret) {
		logger.debug("checkCredentials "+id+" : "+secret);
		return identities.get(id).checkPassword(secret);
	}
	
	// Client claims
	@Override
	public boolean containsToken(String id) {
		logger.debug("containsToken "+id);
		return identities.get(id).containsToken();
	}
	
	@Override
	public Date getTokenExpiringDate(String id) throws SEPASecurityException {
		logger.debug("getTokenExpiringDate "+id);
		return identities.get(id).getTokenExpiringDate();
	}
	
	@Override
	public void addToken(String id, SignedJWT jwt) throws SEPASecurityException {
		logger.debug("addToken "+id+" "+jwt.serialize());
		identities.get(id).addToken(jwt);
	}

	// JWT
	@Override
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException {
		logger.debug("getTokenExpiringPeriod "+id);
		return identities.get(id).getExpiringPeriod();
	}
	
	@Override
	public void setTokenExpiringPeriod(String id, long period) {
		logger.debug("setTokenExpiringPeriod "+id+" : "+period);
		identities.get(id).setExpiringPeriod(period);
	}
	
	@Override
	public SignedJWT getToken(String id) {
		logger.debug("getToken "+id);
		return identities.get(id).getToken();
	}
	@Override
	public DigitalIdentity getIdentity(String uid) throws SEPASecurityException {
		logger.debug("getIdentity "+uid);
		return identities.get(uid).getIdentity();
	}
	
	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		logger.debug("getEndpointCredentials "+uid);
		return identities.get(uid).getIdentity().getEndpointCredentials();			
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
	public String getHttpsAudience() {
		return httpsAudience;
	}
	@Override
	public void setHttpsAudience(String audience) {
		httpsAudience = audience;
	}
	@Override
	public String getWssAudience() {
		return wssAudience;
	}
	@Override
	public void setWssAudience(String audience) {
		wssAudience = audience;
	}
	@Override
	public String getSubject() {
		return subject;
	}
	@Override
	public void setSubject(String sub) {
		subject = sub;
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
}
