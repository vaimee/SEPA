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

package it.unibo.arces.wot.sepa.engine.dependability;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.JWTResponse;
import it.unibo.arces.wot.sepa.commons.response.RegistrationResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.security.Credentials;
import it.unibo.arces.wot.sepa.commons.security.SSLManager;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.IAuthorization;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.InMemoryAuthorization;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.LdapAuthorization;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.identities.ApplicationIdentity;

class SecurityManager {
	private static final Logger logger = LogManager.getLogger();

	// *************************
	// JWT signing and verifying
	// *************************
	private JWSSigner signer;
	private RSASSAVerifier verifier;
	private JsonElement jwkPublicKey;
	private ConfigurableJWTProcessor<SEPASecurityContext> jwtProcessor;
	
	private IAuthorization auth;
	private SSLContext ssl;
	
	public SecurityManager(String keystoreFileName, String keystorePwd, String keyAlias) throws SEPASecurityException {
		auth = new InMemoryAuthorization();
		
		initJWT(keystoreFileName, keystorePwd, keyAlias);
	}
	
	public void enableLDAP(String host, int port, String base, String uid, String pwd) throws SEPASecurityException {
		try {
			logger.info("Create LDAP authorization: "+host+":"+port+" DN: "+base+" UID: "+uid+" PWD: "+pwd);
			auth = new LdapAuthorization(host, port, base, uid, pwd);
		} catch (LdapException e1) {
			logger.error(e1.getMessage());
			throw new SEPASecurityException(e1);
		}
	}
	
	public SSLContext getSSLContext() {
		return ssl;
	}
	
	public void setSSLContextFromJKS(String jksName, String jksPassword) throws SEPASecurityException {
		ssl = new SSLManager().getSSLContextFromJKS(jksName, jksPassword);
	}

	private void securityCheck(String identity) throws SEPASecurityException {
		logger.info("*** Security check ***");
		// Add identity
		auth.addAuthorizedIdentity(new ApplicationIdentity(identity));

		// Register
		Response response = register(identity);
		if (response.getClass().equals(RegistrationResponse.class)) {
			RegistrationResponse ret = (RegistrationResponse) response;
			String basicAuth = ret.getClientId() + ":" + ret.getClientSecret();

			// Get token
			String encodedCredentials = Base64.getEncoder().encodeToString(basicAuth.getBytes());
			logger.debug("Authorization Basic " + encodedCredentials);
			response = getToken(encodedCredentials);

			if (response.getClass().equals(JWTResponse.class)) {
				logger.debug("Access token: " + ((JWTResponse) response).getAccessToken());

				// Validate token
				ClientAuthorization authRet = validateToken(((JWTResponse) response).getAccessToken());
				if (authRet.isAuthorized()) {
					auth.removeCredentials(new ApplicationIdentity(ret.getClientId()));
					auth.removeToken(ret.getClientId());
					logger.info("*** PASSED ***");
				}
				else {
					logger.error(authRet.getError());
					logger.info("*** FAILED ***");
				}
			} else {
				logger.debug(response.toString());
				logger.info("*** FAILED ***");
			}
		} else {
			logger.debug(response.toString());
			logger.info("*** FAILED ***");
			// Remove identity
			auth.removeAuthorizedIdentity(identity);
		}
		System.out.println("");
	}

	private void initJWT(String keystoreFileName, String keystorePwd, String keyAlias) throws SEPASecurityException {
		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream(keystoreFileName), keystorePwd.toCharArray());
			
			RSAKey jwk = RSAKey.load(keystore, keyAlias, keystorePwd.toCharArray());
			
			// Get the private and public keys to sign and verify
			RSAPrivateKey privateKey = jwk.toRSAPrivateKey();
			RSAPublicKey publicKey = jwk.toRSAPublicKey();
			
			// Create RSA-signer with the private key
			signer = new RSASSASigner(privateKey);

			// Create RSA-verifier with the public key
			verifier = new RSASSAVerifier(publicKey);

			// Serialize the public key to be deliverer during registration
			jwkPublicKey = new JsonParser().parse(jwk.toPublicJWK().toJSONString());

			// Set up a JWT processor to parse the tokens and then check their signature
			// and validity time window (bounded by the "iat", "nbf" and "exp" claims)
			jwtProcessor = new DefaultJWTProcessor<SEPASecurityContext>();
			JWKSet jws = new JWKSet(jwk);
			JWKSource<SEPASecurityContext> keySource = new ImmutableJWKSet<SEPASecurityContext>(jws);
			JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
			JWSKeySelector<SEPASecurityContext> keySelector = new JWSVerificationKeySelector<SEPASecurityContext>(
					expectedJWSAlg, keySource);
			jwtProcessor.setJWSKeySelector(keySelector);
			
			securityCheck(UUID.randomUUID().toString());
			
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | JOSEException e) {
			logger.error(e.getMessage());
			if (logger.isTraceEnabled()) e.printStackTrace();
			throw new SEPASecurityException(e.getMessage());
		}
		


	}

	/**
	 * <pre>
	 * POST https://wot.arces.unibo.it:8443/oauth/token
	 * 
	 * Accept: application/json
	 * Content-Type: application/json
	 * 
	 * { 
	 *  "client_identity": ”<ClientIdentity>", 
	 *  "grant_types": ["client_credentials"] 
	 * }
	 * 
	 * Response example:
	 *
	 * {
	 *  "clientId": "889d02cf-16dd-4934-9341-a754088faxyz",
	 *  "clientSecret": "ahd5MU42J0hIxPXzhUhjJHt2d0Oc5M6B644CtuwUlE9zpSuF14-kXYZ",
	 *  "signature" : JWK RSA public key (can be used to verify the signature),
	 *  "authorized" : Boolean
	 * }
	 * 
	 * In case of error, the following applies:
	{
	"error":"Unless specified otherwise see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol",
	"error_description":"Unless specified otherwise, see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol", (OPTIONAL)
	"status_code" : the HTTP status code (would be 400 for Oauth 2.0 errors).
	}
	 * </pre>
	 * 
	 * Create client credentials for an authorized identity
	 * 
	 * @param identity the client identity to be registered
	 * @throws SEPASecurityException
	 */
	public synchronized Response register(String uid) {
		logger.info("REGISTER: " + uid);

		// Check if entity is authorized to request credentials
		try {
			if (!auth.isAuthorized(uid)) {
				logger.warn("Not authorized identity " + uid);
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not_authorized_identity",
						"Client " + uid + " is not authorized");
			}
		} catch (SEPASecurityException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "not_authorized_identity",
					"Exception on authorizing client " + uid+ " "+e.getMessage());
		}

		// Generate password
		String client_secret = UUID.randomUUID().toString();
		boolean forTesting = false;
		try {
			forTesting = auth.isForTesting(uid);
		} catch (SEPASecurityException e1) {
			logger.error(e1.getMessage());
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "check_for_testing",
			
					"Exception on for test checking " + uid+ " "+e1.getMessage());
		}
		if (forTesting) client_secret = uid;
		
		// Store credentials
		 try {
			boolean res =auth.storeCredentials(auth.getIdentity(uid), client_secret);
			if (!res) {
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "storing_credentials",
						"Failed to store credentials for uid:"+uid);
			}
		} catch (SEPASecurityException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "storing_credentials",
					"Exception on storing credentials " + uid+ " "+e.getMessage());
		}
		
		// One time registration (not removed for testing purposes)
		if (!forTesting)
			try {
				auth.removeAuthorizedIdentity(uid);
			} catch (SEPASecurityException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "remove_identity",
						"Exception on removing identity " + uid+ " "+e.getMessage());
			}

		return new RegistrationResponse(uid, client_secret, jwkPublicKey);
	}

	/**
	 * It requests a token to the Authorization Server. A token request should be
	 * made when the current token is expired or it is the first token. If the token
	 * is not expired, the "invalid_grant" error is returned.
	 * 
	 * @param encodedCredentials the client credentials encoded using Base64
	 * @return JWTResponse in case of success, ErrorResponse otherwise
	 * @throws SEPASecurityException
	 * @see JWTResponse
	 * @see ErrorResponse
	 * 
	 *    
	<pre>
	POST https://wot.arces.unibo.it:8443/oauth/token
	 
	Content-Type: application/x-www-form-urlencoded
	Accept: application/json
	Authorization: Basic Basic64(id:secret)
	 
	Response example:
	{
	"access_token": "eyJraWQiOiIyN.........",
	"token_type": "bearer",
	"expires_in": 3600 
	}
	
	 Error response example:
	 {
	   "error":"Unless specified otherwise see RFC6749. Otherwise, this is specific of the SPARQL 1.1 SE Protocol",
	   "error_description":"Unless specified otherwise, see RFC6749. Otherwise, this is specific to the SPARQL 1.1 SE Protocol", (OPTIONAL)
	   "status_code" : the HTTP status code (should be 400 for all Oauth 2.0 errors).
	 }
	
	According to RFC6749, the error member can assume the following values: invalid_request, invalid_client, invalid_grant, unauthorized_client, unsupported_grant_type, invalid_scope.
	
	invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.
		</pre>
	 */

	public synchronized Response getToken(String encodedCredentials) {
		logger.debug("GET TOKEN");

		// Decode credentials
		byte[] decoded = null;
		try {
			decoded = Base64.getDecoder().decode(encodedCredentials);
		} catch (IllegalArgumentException e) {
			logger.error("Not authorized");
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "invalid_request", e.getMessage());
		}

		// Parse credentials
		String decodedCredentials = new String(decoded);
		
		// BUG SOLVED. The clientID may contain ":"
		// e.g., urn:epc:id:gid:0.1.410D23751450344850323220
		int marker = decodedCredentials.lastIndexOf(":");
		if (marker == -1) {
			logger.error("Wrong Basic authorization");
			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "invalid_client",
					"Delimiter ':' is missing. Wrong credentials format: " + decodedCredentials);
		}
		
//		String[] clientID = decodedCredentials.split(":");
//		if (clientID == null) {
//			logger.error("Wrong Basic authorization");
//			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "invalid_client",
//					"Client id not found: " + decodedCredentials);
//		}
//		if (clientID.length != 2) {
//			logger.error("Wrong Basic authorization");
//			return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "invalid_client",
//					"Wrong credentials: " + decodedCredentials);
//		}
//		String id = decodedCredentials.split(":")[0];
//		String secret = decodedCredentials.split(":")[1];
		
		String id = decodedCredentials.substring(0, marker);
		String secret = decodedCredentials.substring(marker+1);		
		
		
		logger.debug("Credentials: " + id + " " + secret);

		// Verify credentials
		try {
			if (!auth.containsCredentials(id)) {
				logger.error("Client id: " + id + " is not registered");
				return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unauthorized_client", "Client identity " + id + " not found");
			}
		} catch (SEPASecurityException e2) {
			return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unauthorized_client", e2.getMessage());
		}

		try {
			if (!auth.checkCredentials(id, secret)) {
				logger.error("Wrong secret: " + secret + " for client id: " + id);
				return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unauthorized_client", "Wrong credentials for identity " + id);
			}
		} catch (SEPASecurityException e2) {
			return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "unauthorized_client", e2.getMessage());
		}

		// Prepare JWT with claims set
		JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
		Date now = new Date();

		// Check if the token is not expired
		try {
			if (auth.containsToken(id)) {
				Date expiring = auth.getTokenExpiringDate(id);
				long expiringUnixSeconds = (expiring.getTime() / 1000) * 1000;
				long nowUnixSeconds = (now.getTime() / 1000) * 1000;
				long delta = expiringUnixSeconds - nowUnixSeconds;
				
				// Expires if major than current time
				logger.debug("ID: " + id + " ==> Token will expire in: " + delta + " ms");
				if (delta > 0) {
					logger.warn("Token is NOT EXPIRED. Return the current token.");
					
					JWTResponse jwt = null;
					try {
						jwt = new JWTResponse(auth.getToken(id));
					} catch (SEPASecurityException e) {
						logger.error(e.getMessage());
						return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "security_error",
								"Failed to retrieve expiring period");
					}
					
					return jwt;
				}
				logger.debug("Token is EXPIRED. Release a fresh token.");
			}
		} catch (SEPASecurityException e2) {
			return new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "security_error", e2.getMessage());
		}

		/*
		 * 4.1.1. "iss" (Issuer) Claim
		 * 
		 * The "iss" (issuer) claim identifies the principal that issued the JWT. The
		 * processing of this claim is generally application specific. The "iss" value
		 * is a case-sensitive string containing a StringOrURI value. Use of this claim
		 * is OPTIONAL.
		 */

//		try {
//			claimsSetBuilder.issuer(auth.getIssuer());
//		} catch (SEPASecurityException e1) {
//			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "invalid_issuer",
//					e1.getMessage());
//		}

		/*
		 * 4.1.2. "sub" (Subject) Claim
		 * 
		 * The "sub" (subject) claim identifies the principal that is the subject of the
		 * JWT. The Claims in a JWT are normally statements about the subject. The
		 * subject value MUST either be scoped to be locally unique in the context of
		 * the issuer or be globally unique. The processing of this claim is generally
		 * application specific. The "sub" value is a case-sensitive string containing a
		 * StringOrURI value. Use of this claim is OPTIONAL.
		 */

		claimsSetBuilder.subject(id);

		/*
		 * 4.1.3. "aud" (Audience) Claim
		 * 
		 * The "aud" (audience) claim identifies the recipients that the JWT is intended
		 * for. Each principal intended to process the JWT MUST identify itself with a
		 * value in the audience claim. If the principal processing the claim does not
		 * identify itself with a value in the "aud" claim when this claim is present,
		 * then the JWT MUST be rejected. In the general case, the "aud" value is an
		 * array of case- sensitive strings, each containing a StringOrURI value. In the
		 * special case when the JWT has one audience, the "aud" value MAY be a single
		 * case-sensitive string containing a StringOrURI value. The interpretation of
		 * audience values is generally application specific. Use of this claim is
		 * OPTIONAL.
		 */

//		ArrayList<String> audience = new ArrayList<String>();
//		try {
//			audience.add(auth.getHttpsAudience());
//		} catch (SEPASecurityException e1) {
//			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "invalid_https_audience",
//					e1.getMessage());
//		}
//		try {
//			audience.add(auth.getWssAudience());
//		} catch (SEPASecurityException e1) {
//			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "invalid_wss_audience",
//					e1.getMessage());
//		}
//		claimsSetBuilder.audience(audience);

		/*
		 * 4.1.4. "exp" (Expiration Time) Claim
		 * 
		 * The "exp" (expiration time) claim identifies the expiration time on or after
		 * which the JWT MUST NOT be accepted for processing. The processing of the
		 * "exp" claim requires that the current date/time MUST be before the expiration
		 * date/time listed in the "exp" claim. Implementers MAY provide for some small
		 * leeway, usually no more than a few minutes, to account for clock skew. Its
		 * value MUST be a number containing a NumericDate value. Use of this claim is
		 * OPTIONAL.
		 */

		/*
		 * NOTICE: this date is serialized as SECONDS from UNIX time NOT milliseconds!
		 */
		// Define the expiration time
		Date expires;
		try {
			expires = new Date(now.getTime() + (auth.getTokenExpiringPeriod(id) * 1000));
		} catch (SEPASecurityException e1) {
			logger.error(e1.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "security_error",
					"Failed to retrieve expiring period");
		}
		claimsSetBuilder.expirationTime(expires);

		/*
		 * 4.1.5. "nbf" (Not Before) Claim
		 * 
		 * The "nbf" (not before) claim identifies the time before which the JWT MUST
		 * NOT be accepted for processing. The processing of the "nbf" claim requires
		 * that the current date/time MUST be after or equal to the not-before date/time
		 * listed in the "nbf" claim. Implementers MAY provide for some small leeway,
		 * usually no more than a few minutes, to account for clock skew. Its value MUST
		 * be a number containing a NumericDate value. Use of this claim is OPTIONAL.
		 */

		// claimsSetBuilder.notBeforeTime(before);

		/*
		 * 4.1.6. "iat" (Issued At) Claim
		 * 
		 * The "iat" (issued at) claim identifies the time at which the JWT was issued.
		 * This claim can be used to determine the age of the JWT. Its value MUST be a
		 * number containing a NumericDate value. Use of this claim is OPTIONAL.
		 */

		claimsSetBuilder.issueTime(now);

		/*
		 * 4.1.7. "jti" (JWT ID) Claim
		 * 
		 * The "jti" (JWT ID) claim provides a unique identifier for the JWT. The
		 * identifier value MUST be assigned in a manner that ensures that there is a
		 * negligible probability that the same value will be accidentally assigned to a
		 * different data object; if the application uses multiple issuers, collisions
		 * MUST be prevented among values produced by different issuers as well. The
		 * "jti" claim can be used to prevent the JWT from being replayed. The "jti"
		 * value is a case- sensitive string. Use of this claim is OPTIONAL.
		 */

		//claimsSetBuilder.jwtID(id);

		JWTClaimsSet jwtClaims = claimsSetBuilder.build();

		// ******************************
		// Sign JWT with private RSA key
		// ******************************
		SignedJWT signedJWT;
		try {
			signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), JWTClaimsSet.parse(jwtClaims.toString()));
		} catch (ParseException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "parsing_exception", "ParseException: " + e.getMessage());
		}
		try {
			signedJWT.sign(signer);
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "sign_exception", "JOSEException: " + e.getMessage());
		}

		// Add the token to the released tokens
		try {
			auth.addToken(id, signedJWT);
		} catch (SEPASecurityException e1) {
			logger.error(e1.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "security_error",
					e1.getMessage());
		}

		JWTResponse jwt = null;
		try {
			jwt = new JWTResponse(signedJWT);
		} catch (SEPASecurityException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "security_error",
					"Failed to retrieve expiring period");
		}
		logger.debug("Released token: " + jwt);
		
		return jwt;
	}

	/**
	 * Operation when receiving a request at a protected endpoint
	 * 
	 * <pre>
	Specific to HTTP request:
	1. Check if the request contains an Authorization header. 
	2. Check if the request contains an Authorization: Bearer-header with non-null/empty contents 
	3. Check if the value of the Authorization: Bearer-header is a JWT object 
	
	Token validation:
	4. Check if the JWT object is signed 
	5. Check if the signature of the JWT object is valid. This is to be checked with AS public signature verification key 
	6. Check the contents of the JWT object 
	7. Check if the value of "iss" is https://wot.arces.unibo.it:8443/oauth/token 
	8. Check if the value of "aud" contains https://wot.arces.unibo.it:8443/sparql 
	9. Accept the request as well as "sub" as the originator of the request and process it as usual
	 
	Respond with 401 if not
	
	According to RFC6749, the error member can assume the following values: invalid_request, invalid_client, invalid_grant, unauthorized_client, unsupported_grant_type, invalid_scope.
	
	     invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.
	 * 
	 * </pre>
	 * 
	 * @param accessToken the JWT token to be validate according to points 4-6
	 */
	public synchronized ClientAuthorization validateToken(String accessToken) {
		logger.debug("VALIDATE TOKEN");

		// Parse token
		SignedJWT signedJWT = null;
		try {
			signedJWT = SignedJWT.parse(accessToken);
		} catch (ParseException e) {
			logger.error(e.getMessage());
			return new ClientAuthorization("invalid_request","ParseException: " + e.getMessage());
		}

		// Verify token
		try {
			if (!signedJWT.verify(verifier)) {
				logger.error("Signed JWT not verified");
				return new ClientAuthorization("invalid_grant","Signed JWT not verified");
			}

		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return new ClientAuthorization("invalid_grant","JOSEException: " + e.getMessage());
		}

		// Process token (validate)
		JWTClaimsSet claimsSet = null;
		try {
			claimsSet = jwtProcessor.process(accessToken, new SEPASecurityContext());
		} catch (ParseException e) {
			logger.error(e.getMessage());
			return new ClientAuthorization("invalid_grant","ParseException. " + e.getMessage());
		} catch (BadJOSEException e) {
			logger.error(e.getMessage());
			return new ClientAuthorization("invalid_grant","BadJOSEException. " + e.getMessage());
		} catch (JOSEException e) {
			logger.error(e.getMessage());
			return new ClientAuthorization("invalid_grant","JOSEException. " + e.getMessage());
		}

		// Check token expiration (an "invalid_grant" error is raised if the token is expired)
		Date now = new Date();
		long nowUnixSeconds = (now.getTime() / 1000) * 1000;
		Date expiring = claimsSet.getExpirationTime();
		Date notBefore = claimsSet.getNotBeforeTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		if (expiring.getTime() - nowUnixSeconds < 0) {
			logger.warn("Token is expired: " + sdf.format(claimsSet.getExpirationTime()) + " < "
					+ sdf.format(new Date(nowUnixSeconds)));
			
			return new ClientAuthorization("invalid_grant","Token issued at "+sdf.format(claimsSet.getIssueTime())+" is expired: " + sdf.format(claimsSet.getExpirationTime())
					+ " < " + sdf.format(now));
		}

		if (notBefore != null && nowUnixSeconds < notBefore.getTime()) {
			logger.warn("Token can not be used before: " + claimsSet.getNotBeforeTime());
			return new ClientAuthorization("invalid_grant","Token can not be used before: " + claimsSet.getNotBeforeTime());
		}
		
		// Get client credentials for accessing the SPARQL endpoint
		String id = claimsSet.getSubject();
		
		logger.debug("Get credentials for uid: "+id);
		
		Credentials cred = null;
		try {
			cred = auth.getEndpointCredentials(id);
		} catch (SEPASecurityException e) {
			logger.error("Failed to retrieve credentials ("+id+")");
			return new ClientAuthorization("invalid_grant","Failed to get credentials ("+id+")");
		}

		return new ClientAuthorization(cred);
	}
}
