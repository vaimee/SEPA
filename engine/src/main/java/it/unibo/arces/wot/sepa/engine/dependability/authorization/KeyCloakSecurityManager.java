package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpStatus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.security.Credentials;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.identities.DigitalIdentity;
import it.unibo.arces.wot.sepa.logging.Logging;

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
	 * 
	 * If you want to validate these tokens without a call to the remote introspection endpoint, you can decode the RPT and query for its validity locally. 
	 * Once you decode the token, you can also use the permissions within the token to enforce authorization decisions.
	 * 
	 * This is essentially what the policy enforcers do. Be sure to:
	 * 1) Validate the signature of the RPT (based on the realm’s public key)
	 * 2) Query for token validity based on its exp, iat, and aud claims
	 * 
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
	public void addAuthorizedIdentity(DigitalIdentity identity) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAuthorizedIdentity(String uid) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DigitalIdentity getIdentity(String uid) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAuthorized(String identity) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isForTesting(String identity) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storeCredentials(DigitalIdentity identity, String secret) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeCredentials(DigitalIdentity identity) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsCredentials(String uid) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean checkCredentials(String uid, String secret) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Credentials getEndpointCredentials(String uid) throws SEPASecurityException {
		return new Credentials(uid, ldap.getEndpointUsersPassword());
	}

	@Override
	public void addJwt(String id, SignedJWT claims) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean containsJwt(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public SignedJWT getJwt(String uid) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeJwt(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Date getTokenExpiringDate(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getTokenExpiringPeriod(String id) throws SEPASecurityException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTokenExpiringPeriod(String id, long period) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDeviceExpiringPeriod(long period) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDeviceExpiringPeriod() throws SEPASecurityException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setApplicationExpiringPeriod(long period) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getApplicationExpiringPeriod() throws SEPASecurityException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setUserExpiringPeriod(long period) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getUserExpiringPeriod() throws SEPASecurityException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setDefaultExpiringPeriod(long period) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getDefaultExpiringPeriod() throws SEPASecurityException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getIssuer() throws SEPASecurityException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setIssuer(String is) throws SEPASecurityException {
		// TODO Auto-generated method stub
		
	}

}
