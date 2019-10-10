package it.unibo.arces.wot.sepa.engine.dependability;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class InMemoryAuthorization implements IClientAuthorization {
	// UID authorized requesting credentials
	private final List<String> authorizedIdentities = new ArrayList<String>();

	// ID ==> Secret
	private final HashMap<String, String> credentials = new HashMap<String, String>();

	// ID ==> JWT
	private final HashMap<String, SignedJWT> tokens = new HashMap<String, SignedJWT>();

	private long expiring = 5;

	private String issuer = "https://wot.arces.unibo.it:8443/oauth/token";
	private String httpsAudience = "https://wot.arces.unibo.it:8443/sparql";
	private String wssAudience = "wss://wot.arces.unibo.it:9443/sparql";
	private String subject = "SEPATest";
	
	// Identities authorized retrieving client credentials
	@Override
	public boolean isAuthorized(String id) throws SEPASecurityException {
		return authorizedIdentities.contains(id);
	}
	@Override
	public void addAuthorizedIdentity(String id) {
		authorizedIdentities.add(id);
	}
	@Override
	public void removeAuthorizedIdentity(String id) {
		authorizedIdentities.remove(id);
	}
	
	// Client credentials
	@Override
	public void storeCredentials(String client_id, String client_secret) {
		while (credentials.containsKey(client_id))
			client_id = UUID.randomUUID().toString();
		credentials.put(client_id, client_secret);

	}
	@Override
	public boolean isClientRegistered(String id) {
		return credentials.containsKey(id);
	}
	@Override
	public boolean passwordCheck(String id, String secret) {
		return credentials.get(id).equals(secret);
	}
	
	// Client claims
	@Override
	public boolean containsToken(String id) {
		return tokens.containsKey(id);
	}
	@Override
	public Date getExpiringTime(String id) throws SEPASecurityException {
		SignedJWT token = tokens.get(id);
		JWTClaimsSet claims = null;
		try {
			claims = token.getJWTClaimsSet();
		} catch (ParseException e) {
			throw new SEPASecurityException(e);
		}
		
		return claims.getExpirationTime();
	}
	@Override
	public void addToken(String id, SignedJWT jwt) {
		tokens.put(id, jwt);
	}

	// JWT
	@Override
	public long getTokenExpiringPeriod(String id) {
		return expiring;
	}
	@Override
	public void setTokenExpiringPeriod(String id, long period) {
		expiring = period;
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
	public SignedJWT getToken(String id) {
		return tokens.get(id);
	}
}
