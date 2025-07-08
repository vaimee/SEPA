package com.vaimee.sepa.engine.dependability.authorization;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpStatus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;
import com.vaimee.sepa.api.commons.security.Credentials;
import com.vaimee.sepa.engine.dependability.authorization.identities.DigitalIdentity;
import com.vaimee.sepa.logging.Logging;

public class KeyCloakSecurityManager extends SecurityManager {

	private SyncLdap ldap;
	private VirtuosoIsql isql;

	public KeyCloakSecurityManager(SSLContext ssl, RSAKey key,LdapProperties prop, IsqlProperties isqlprop)
			throws SEPASecurityException {
		super(ssl, key, false);

		ldap = new SyncLdap(prop);

		isql = new VirtuosoIsql(isqlprop,ldap.getEndpointUsersPassword());

		new UsersSync(ldap, isql);
		
		Logging.logger.log(Logging.getLevel("oauth"),"EndpointUsersPassword: "+ldap.getEndpointUsersPassword());
	}
	
	@Override
	public synchronized Response register(String uid) {
		return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not supported", "Implemented by KeyCloak");
	}
	
	@Override
	public synchronized Response getToken(String encodedCredentials) {
		return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not supported", "Implemented by KeyCloak");
	}

	/** Requesting Party Token 
	 * <p>
	 * If you want to validate these tokens without a call to the remote introspection endpoint, you can decode the RPT and query for its validity locally. 
	 * Once you decode the token, you can also use the permissions within the token to enforce authorization decisions.
	 * <p>
	 * This is essentially what the policy enforcers do. Be sure to:
	 * 1) Validate the signature of the RPT (based on the realm’s public key)
	 * 2) Query for token validity based on its exp, iat, and aud claims
	 * <p>
	 * The claim "preferred_username" is used to identify the user
	 * */
	@Override
	public synchronized ClientAuthorization validateToken(String accessToken) {
		Logging.logger.log(Logging.getLevel("oauth"),"VALIDATE TOKEN");

		// Parse token
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(accessToken);
		} catch (ParseException e) {
			Logging.logger.log(Logging.getLevel("oauth"),e.getMessage());
			return new ClientAuthorization("invalid_request", "ParseException: " + e.getMessage());
		}

		// Verify token
		try {
			if (!signedJWT.verify(verifier)) {
				Logging.logger.log(Logging.getLevel("oauth"),"Signed JWT not verified");
				return new ClientAuthorization("invalid_grant", "Signed JWT not verified");
			}

		} catch (JOSEException e) {
			Logging.logger.log(Logging.getLevel("oauth"),e.getMessage());
			return new ClientAuthorization("invalid_grant", "JOSEException: " + e.getMessage());
		}
		

		String uid;
		// Process token (validate)
		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = signedJWT.getJWTClaimsSet();
			Logging.logger.log(Logging.getLevel("oauth"),claimsSet);
			// Get client credentials for accessing the SPARQL endpoint
			uid = claimsSet.getStringClaim("username");
			if (uid == null) {
				Logging.logger.log(Logging.getLevel("oauth"),"<username> claim is null. Look for <preferred_username>");
				uid = claimsSet.getStringClaim("preferred_username");
				if (uid == null) {
					Logging.logger.log(Logging.getLevel("oauth"),"USER ID not found...");
					return new ClientAuthorization("invalid_grant", "Username claim not found");
				}
			}
			
			Logging.logger.log(Logging.getLevel("oauth"),"Subject: "+claimsSet.getSubject());
			Logging.logger.log(Logging.getLevel("oauth"),"Issuer: "+claimsSet.getIssuer());
			Logging.logger.log(Logging.getLevel("oauth"),"Username: "+uid);
		} catch (ParseException e) {
			Logging.logger.error(e.getMessage());
			return new ClientAuthorization("invalid_grant", "ParseException. " + e.getMessage());
		}


		// Check token expiration (an "invalid_grant" error is raised if the token is
		// expired)
		Date now = new Date();
		long nowUnixSeconds = (now.getTime() / 1000) * 1000;
		Date expiring = claimsSet.getExpirationTime();
		Date notBefore = claimsSet.getNotBeforeTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		if (expiring.getTime() - nowUnixSeconds < 0) {
			Logging.logger.log(Logging.getLevel("oauth"),"Token is expired: " + sdf.format(claimsSet.getExpirationTime()) + " < "
					+ sdf.format(new Date(nowUnixSeconds)));

			return new ClientAuthorization("invalid_grant", "Token issued at " + sdf.format(claimsSet.getIssueTime())
					+ " is expired: " + sdf.format(claimsSet.getExpirationTime()) + " < " + sdf.format(now));
		}

		if (notBefore != null && nowUnixSeconds < notBefore.getTime()) {
			Logging.logger.log(Logging.getLevel("oauth"),"Token can not be used before: " + claimsSet.getNotBeforeTime());
			return new ClientAuthorization("invalid_grant",
					"Token can not be used before: " + claimsSet.getNotBeforeTime());
		}
		
		Credentials cred = null;
		try {
			cred = getEndpointCredentials(uid);
			Logging.logger.log(Logging.getLevel("oauth"),"Endpoint credentials: "+cred);
		} catch (SEPASecurityException e) {
			Logging.logger.log(Logging.getLevel("oauth"),"Failed to retrieve credentials (" + uid + ")");
			return new ClientAuthorization("invalid_grant", "Failed to get credentials (" + uid + ")");
		}

		return new ClientAuthorization(cred);
	}

	@Override
	public void addAuthorizedIdentity(DigitalIdentity identity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAuthorizedIdentity(String uid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DigitalIdentity getIdentity(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAuthorized(String identity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isForTesting(String identity) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storeCredentials(DigitalIdentity identity, String secret) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeCredentials(DigitalIdentity identity) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsCredentials(String uid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkCredentials(String uid, String secret) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		return new Credentials(uid, ldap.getEndpointUsersPassword());
	}

	@Override
	public void addJwt(String id, SignedJWT claims) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsJwt(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SignedJWT getJwt(String uid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeJwt(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Date getTokenExpiringDate(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTokenExpiringPeriod(String id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTokenExpiringPeriod(String id, long period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeviceExpiringPeriod(long period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDeviceExpiringPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setApplicationExpiringPeriod(long period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getApplicationExpiringPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setUserExpiringPeriod(long period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getUserExpiringPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setDefaultExpiringPeriod(long period) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDefaultExpiringPeriod() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getIssuer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIssuer(String is) {
		// TODO Auto-generated method stub
		
	}

}
