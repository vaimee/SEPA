/* This class implements the OAuth 2.0 Authorization Manager (AM) of the SEPA
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import com.sun.net.httpserver.HttpsConfigurator;

import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;
import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class AuthorizationManager implements AuthorizationManagerMBean {
	
	//TODO: CLIENTS DB to be made persistent
	//IDENTITY ==> ID
	private HashMap<String,String> clients = new HashMap<String,String>();
	
	//TODO: CREDENTIALS DB to be made persistent
	//ID ==> Secret
	private HashMap<String,String> credentials = new HashMap<String,String>();
	
	//TODO: TOKENS DB to be made persistent
	//ID ==> JWTClaimsSet
	private HashMap<String,JWTClaimsSet> clientClaims = new HashMap<String,JWTClaimsSet>();
	
	//*************************
	//JWT signing and verifying
	//*************************
	private JWSSigner signer;
	private RSASSAVerifier verifier;
	private JsonElement jwkPublicKey;
	private ConfigurableJWTProcessor<SEPASecurityContext> jwtProcessor;
	private SEPASecurityContext context = new SEPASecurityContext();
	private SSLSecurityManager sManager;
	
	//JMX Access
	private HashMap<String,Boolean> authorizedIdentities = new HashMap<String,Boolean>();
	private long expiring = 5; 												
	private String issuer = "https://wot.arces.unibo.it:8443/oauth/token"; 		
	private String httpsAudience = "https://wot.arces.unibo.it:8443/sparql"; 	
	private String wssAudience ="wss://wot.arces.unibo.it:9443/sparql";  		
	private String subject = "SEPATest";										
	
	private static final Logger logger = LogManager.getLogger("AuthorizationManager");
	
	/**
	Security context. Provides additional information necessary for processing a JOSE object.
	Example context information:

	Identifier of the message producer (e.g. OpenID Connect issuer) to retrieve its public key to verify the JWS signature.
	Indicator whether the message was received over a secure channel (e.g. TLS/SSL) which is essential for processing unsecured (plain) JOSE objects.
	*/
	private class SEPASecurityContext implements SecurityContext {
		
	}
	
	public HttpsConfigurator getHttpsConfigurator() {
		return sManager.getHttpsConfigurator();
	} 
	
	private void securityCheck(String identity) {
		logger.debug("*** Security check ***");
		//Add identity
		addAuthorizedIdentity(identity);
		
		//Register
		logger.debug("Register: "+identity);
		Response response = register(identity);
		if (response.getClass().equals(RegistrationResponse.class)) {
			RegistrationResponse ret = (RegistrationResponse) response;
			String auth = ret.getClientId()+":"+ret.getClientSecret();	
			logger.debug("ID:SECRET="+auth);
			
			//Get token
			String encodedCredentials = Base64.getEncoder().encodeToString(auth.getBytes());
			logger.debug("Authorization Basic "+encodedCredentials);
			response = getToken(encodedCredentials);
			
			if (response.getClass().equals(JWTResponse.class)) {
				logger.debug("Access token: "+((JWTResponse) response).getAccessToken());
				
				//Validate token
				Response valid = validateToken(((JWTResponse) response).getAccessToken());
				if(!valid.getClass().equals(ErrorResponse.class)) logger.debug("PASSED");
				else {
					ErrorResponse error = (ErrorResponse) valid;
					logger.debug("FAILED Code: "+error.getErrorCode()+ "Message: "+error.getErrorMessage());
				}
			}
			else logger.debug("FAILED");
		}
		else logger.debug("FAILED");
		logger.debug("**********************");
		System.out.println("");	
		
		//Add identity
		removeAuthorizedIdentity(identity);
	}

	private boolean init(String keyAlias,String keyPwd){		
		// Load the key from the key store
		RSAKey jwk = sManager.getJWK(keyAlias,keyPwd);
						
		//Get the private and public keys to sign and verify
		RSAPrivateKey privateKey;
		RSAPublicKey publicKey;
		try {
			privateKey = jwk.toRSAPrivateKey();
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		try {
			publicKey = jwk.toRSAPublicKey();
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return false;
		}
		
		// Create RSA-signer with the private key
		signer = new RSASSASigner(privateKey);
		
		// Create RSA-verifier with the public key
		verifier = new RSASSAVerifier(publicKey);
				
		//Serialize the public key to be deliverer during registration
		jwkPublicKey = new JsonParser().parse(jwk.toPublicJWK().toJSONString());
		
		// Set up a JWT processor to parse the tokens and then check their signature
		// and validity time window (bounded by the "iat", "nbf" and "exp" claims)
		jwtProcessor = new DefaultJWTProcessor<SEPASecurityContext>();
		JWKSet jws = new JWKSet(jwk);
		JWKSource<SEPASecurityContext> keySource = new ImmutableJWKSet<SEPASecurityContext>(jws);
		JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
		JWSKeySelector<SEPASecurityContext> keySelector = new JWSVerificationKeySelector<SEPASecurityContext>(expectedJWSAlg, keySource);
		jwtProcessor.setJWSKeySelector(keySelector);
		
		return true;
	}
	
	public AuthorizationManager(String keystoreFileName,String keystorePwd,String keyAlias,String keyPwd,String certificate) {	
		SEPABeans.registerMBean("SEPA:type=AuthorizationManager",this);	
		
		sManager = new SSLSecurityManager(keystoreFileName, keystorePwd, keyAlias, keyPwd, certificate,false,true,null);
		init(keyAlias, keyPwd);
		
		securityCheck(UUID.randomUUID().toString());
	}
	
	private boolean authorizeIdentity(String id) {
		logger.debug("Authorize identity:"+id);
		
		//TODO: WARNING! TO BE REMOVED IN PRODUCTION. ONLY FOR TESTING.
		if (id.equals("SEPATest")) {
			logger.warn("SEPATest authorized! Setting expiring token period to 5 seconds");
			authorizedIdentities.put(id, true);
			expiring = 5;
			return true;
		}
		
		if(!authorizedIdentities.containsKey(id)) return false;
		
		if(!authorizedIdentities.get(id)) return false;
		
		authorizedIdentities.put(id, false);
		return true;
	}
	
	/**
	 * POST https://wot.arces.unibo.it:8443/oauth/token
	 * 
	 * Accept: application/json
	 * Content-Type: application/json
	 * 
	 * { 
	 * "client_identity": ”<ClientIdentity>", 
	 * "grant_types": ["client_credentials"] 
	 * }
	 * 
	 * Response example:
	 * { 	"client_id": "889d02cf-16dd-4934-9341-a754088faxyz",
	 * 		"client_secret": "ahd5MU42J0hIxPXzhUhjJHt2d0Oc5M6B644CtuwUlE9zpSuF14-kXYZ",
	 * 		"signature" : JWK RSA public key (can be used to verify the signature),
	 * 		"authorized" : Boolean
	 * }
	 * 
	 * In case of error, the following applies:
	 * {
	 * 		"code": Error code,
	 * 		"body": "Error details" (optional)
	 * 
	 * }
	 * */
	public Response register(String identity) {
		logger.debug("Register: "+identity);
		
		//Check if entity is authorized to request credentials
		if (!authorizeIdentity(identity)) {
			logger.error("Not authorized identity "+identity);
			return new ErrorResponse(ErrorResponse.UNAUTHORIZED,"Not authorized identity "+identity);
		}
		
		String client_id = null;
		String client_secret = null;
		
		//Check if identity has been already registered
		if (clients.containsKey(identity)) {
			logger.warn("Giving credentials to a registred identity "+identity);
			client_id = clients.get(identity);
			client_secret = credentials.get(client_id);
		}
		else {
			//Create credentials
			client_id = UUID.randomUUID().toString();
			client_secret = UUID.randomUUID().toString();
		
			//Store credentials
			while(credentials.containsKey(client_id)) client_id = UUID.randomUUID().toString();
			credentials.put(client_id,client_secret);
		
			//Register client
			clients.put(identity, client_id);
		}
		return new RegistrationResponse(client_id,client_secret,jwkPublicKey);
	}
	
	/**
	 * POST https://wot.arces.unibo.it:8443/oauth/token
	 * 
	 * Content-Type: application/x-www-form-urlencoded
	 * Accept: application/json
	 * Authorization: Basic Basic64(id:secret)
	 * 
	 * Response example:
	 * { 	"access_token": "eyJraWQiOiIyN.........",
	 * 		"token_type": "bearer",
	 * 		"expires_in": 3600 
	 * }
	 * 
	 * In case of error, the following applies:
	 * {
	 * 		"code": Error code,
	 * 		"body": "Error details"
	 * 
	 * }
	 * */
	public Response getToken(String encodedCredentials) {
		logger.debug("Get token");
		
		//Decode credentials
		byte[] decoded = null;
		try{
			decoded = Base64.getDecoder().decode(encodedCredentials);
		}
		catch (IllegalArgumentException e) {
			logger.error("Not authorized");
			return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Client not authorized");
		}
		String decodedCredentials = new String(decoded);
		String[] clientID = decodedCredentials.split(":");
		if (clientID==null){
			logger.error("Wrong Basic authorization");
			return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Client not authorized");
		}
		if (clientID.length != 2) {
			logger.error("Wrong Basic authorization");
			return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Client not authorized");
		}
		
		String id = decodedCredentials.split(":")[0];
		String secret = decodedCredentials.split(":")[1];
		logger.debug("Credentials: "+id+" "+secret);
		
		//Verify credentials
		if (!credentials.containsKey(id)) {
			logger.error("Client id: "+id+" is not registered");
			return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Client not authorized");
		}
		
		if (!credentials.get(id).equals(secret)) {
			logger.error("Wrong secret: "+secret+ " for client id: "+id);
			return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Client not authorized");
		}
		
		//Check is a token has been release for this client
		if (clientClaims.containsKey(id)) {
			//Do not return a new token if the previous one is not expired
			Date expires = clientClaims.get(id).getExpirationTime();
			Date now = new Date();
			logger.debug("Check token expiration: "+now+" > "+expires+ " ?");
			if(now.before(expires)) {
				logger.warn("Token is not expired");
				return new ErrorResponse(0,ErrorResponse.BAD_REQUEST,"Token is not expired");
			}
		}
		
		// Prepare JWT with claims set
		 JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
		 long timestamp = new Date().getTime();
		 
		/*
		 * 4.1.1.  "iss" (Issuer) Claim

	   The "iss" (issuer) claim identifies the principal that issued the
	   JWT.  The processing of this claim is generally application specific.
	   The "iss" value is a case-sensitive string containing a StringOrURI
	   value.  Use of this claim is OPTIONAL.*/
		 
		 claimsSetBuilder.issuer(issuer);
		 
	 /* 4.1.2.  "sub" (Subject) Claim

	   The "sub" (subject) claim identifies the principal that is the
	   subject of the JWT.  The Claims in a JWT are normally statements
	   about the subject.  The subject value MUST either be scoped to be
	   locally unique in the context of the issuer or be globally unique.
	   The processing of this claim is generally application specific.  The
	   "sub" value is a case-sensitive string containing a StringOrURI
	   value.  Use of this claim is OPTIONAL.*/
		 
		 claimsSetBuilder.subject(subject);
		
	 /* 4.1.3.  "aud" (Audience) Claim

	   The "aud" (audience) claim identifies the recipients that the JWT is
	   intended for.  Each principal intended to process the JWT MUST
	   identify itself with a value in the audience claim.  If the principal
	   processing the claim does not identify itself with a value in the
	   "aud" claim when this claim is present, then the JWT MUST be
	   rejected.  In the general case, the "aud" value is an array of case-
	   sensitive strings, each containing a StringOrURI value.  In the
	   special case when the JWT has one audience, the "aud" value MAY be a
	   single case-sensitive string containing a StringOrURI value.  The
	   interpretation of audience values is generally application specific.
	   Use of this claim is OPTIONAL.*/
		 
		 ArrayList<String> audience = new ArrayList<String>();
		 audience.add(httpsAudience);
		 audience.add(wssAudience);
		 claimsSetBuilder.audience(audience);
		
		/* 4.1.4.  "exp" (Expiration Time) Claim

	   The "exp" (expiration time) claim identifies the expiration time on
	   or after which the JWT MUST NOT be accepted for processing.  The
	   processing of the "exp" claim requires that the current date/time
	   MUST be before the expiration date/time listed in the "exp" claim.
	   Implementers MAY provide for some small leeway, usually no more than
	   a few minutes, to account for clock skew.  Its value MUST be a number
	   containing a NumericDate value.  Use of this claim is OPTIONAL.*/
		
		 claimsSetBuilder.expirationTime(new Date(timestamp+(expiring*1000)));
		
		/*4.1.5.  "nbf" (Not Before) Claim

	   The "nbf" (not before) claim identifies the time before which the JWT
	   MUST NOT be accepted for processing.  The processing of the "nbf"
	   claim requires that the current date/time MUST be after or equal to
	   the not-before date/time listed in the "nbf" claim.  Implementers MAY
	   provide for some small leeway, usually no more than a few minutes, to
	   account for clock skew.  Its value MUST be a number containing a
	   NumericDate value.  Use of this claim is OPTIONAL.*/
		
		 claimsSetBuilder.notBeforeTime(new Date(timestamp-1000));
		
		/* 4.1.6.  "iat" (Issued At) Claim

	   The "iat" (issued at) claim identifies the time at which the JWT was
	   issued.  This claim can be used to determine the age of the JWT.  Its
	   value MUST be a number containing a NumericDate value.  Use of this
	   claim is OPTIONAL.*/

		claimsSetBuilder.issueTime(new Date(timestamp));
		
		/*4.1.7.  "jti" (JWT ID) Claim

	   The "jti" (JWT ID) claim provides a unique identifier for the JWT.
	   The identifier value MUST be assigned in a manner that ensures that
	   there is a negligible probability that the same value will be
	   accidentally assigned to a different data object; if the application
	   uses multiple issuers, collisions MUST be prevented among values
	   produced by different issuers as well.  The "jti" claim can be used
	   to prevent the JWT from being replayed.  The "jti" value is a case-
	   sensitive string.  Use of this claim is OPTIONAL.*/
		
		claimsSetBuilder.jwtID(id+":"+secret);

		JWTClaimsSet jwtClaims = claimsSetBuilder.build();
		
		//******************************
		// Sign JWT with private RSA key
		//******************************
		SignedJWT signedJWT;
		try {
			signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), JWTClaimsSet.parse(jwtClaims.toString()));
		} catch (ParseException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(0,ErrorResponse.INTERNAL_SERVER_ERROR,"Error on signing JWT (1)");
		}
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(0,ErrorResponse.INTERNAL_SERVER_ERROR,"Error on signing JWT (2)");
		}
						
		//Add the token to the released tokens
		clientClaims.put(id, jwtClaims);
		
		return new JWTResponse(signedJWT.serialize(),"bearer",expiring);
	}
	
	public Response validateToken(String accessToken) {
		logger.debug("Validate token");
		
		//Parse and verify the token
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(accessToken);
		} catch (ParseException e) {
			return new ErrorResponse(ErrorResponse.UNAUTHORIZED,e.getMessage());
		}

		try {
			 if(!signedJWT.verify(verifier)) return new ErrorResponse(ErrorResponse.UNAUTHORIZED);
			 
		} catch (JOSEException e) {
			return new ErrorResponse(ErrorResponse.UNAUTHORIZED,e.getMessage());
		}
		
		// Process the token
		JWTClaimsSet claimsSet;
		try {
			claimsSet = jwtProcessor.process(accessToken, context);
		} catch (ParseException | BadJOSEException | JOSEException e) {
			return new ErrorResponse(ErrorResponse.INTERNAL_SERVER_ERROR,e.getMessage());
		}
		
		//Check token expiration
		Date now = new Date();
		if (now.after(claimsSet.getExpirationTime())) return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Token is expired "+claimsSet.getExpirationTime());
			
		if (now.before(claimsSet.getNotBeforeTime())) return new ErrorResponse(0,ErrorResponse.UNAUTHORIZED,"Token can not be used before: "+claimsSet.getNotBeforeTime());	
				
		return new JWTResponse(accessToken,"bearer",now.getTime()-claimsSet.getExpirationTime().getTime());
	}

	public SSLEngineConfigurator getWssConfigurator() {
		SSLEngineConfigurator config = new SSLEngineConfigurator(sManager.getWssConfigurator().getSslContext(), false, false, false);
		return config;
	}

	
	@Override
	public long getTokenExpiringPeriod() {
		return expiring;
	}
	

	@Override
	public void setTokenExpiringPeriod(long period) {
		expiring = period;
	}

	@Override
	public void addAuthorizedIdentity(String id) {
		authorizedIdentities.put(id, true);
	}

	@Override
	public void removeAuthorizedIdentity(String id) {
		authorizedIdentities.remove(id);
	}

	@Override
	public HashMap<String, Boolean> getAuthorizedIdentities() {
		return authorizedIdentities;
	}

	@Override
	public String getIssuer() {
		return issuer;
	}

	@Override
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	@Override
	public String getHttpsAudience() {
		return httpsAudience;
	}

	@Override
	public void setHttpsAudience(String audience) {
		this.httpsAudience = audience;
	}

	@Override
	public String getWssAudience() {
		return wssAudience;
	}

	@Override
	public void setWssAudience(String audience) {
		this.wssAudience = audience;
	}

	@Override
	public String getSubject() {
		return this.subject;
	}

	@Override
	public void setSubject(String sub) {
		this.subject = sub;
	}	
}
