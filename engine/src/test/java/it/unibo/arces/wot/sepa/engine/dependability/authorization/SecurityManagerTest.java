package it.unibo.arces.wot.sepa.engine.dependability.authorization;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.identities.ApplicationIdentity;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.identities.DeviceIdentity;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.identities.DigitalIdentity;

public class SecurityManagerTest {
	private static SecurityManager auth;
	private static ConfigurationProvider configurationProvider;

	
	private SignedJWT generateToken(DigitalIdentity identity, String password) throws ParseException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, JOSEException, SEPASecurityException {
		// Prepare JWT with claims set
		JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();

		// Define validity period
		Date now = new Date();
		
		long exp = 0;
		if (identity.getClass().equals(DeviceIdentity.class)) {
			exp = auth.getDeviceExpiringPeriod();
		}
		else if (identity.getClass().equals(ApplicationIdentity.class)) {
			exp = auth.getApplicationExpiringPeriod();
		} else exp = auth.getDefaultExpiringPeriod();
		
		Date expires = new Date(now.getTime() + exp * 1000);

		claimsSetBuilder.issuer("http://issuer");
		claimsSetBuilder.subject("http://subject");
		ArrayList<String> audience = new ArrayList<String>();
		audience.add("https://audience");
		audience.add("wss://audience");
		claimsSetBuilder.audience(audience);
		claimsSetBuilder.expirationTime(expires);
		claimsSetBuilder.issueTime(now);
		claimsSetBuilder.jwtID(identity.getUid() + ":" + password + ":" + UUID.randomUUID());

		JWTClaimsSet jwtClaims = claimsSetBuilder.build();

		// ******************************
		// Sign JWT with private RSA key
		// ******************************
		SignedJWT signedJWT;
		signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), JWTClaimsSet.parse(jwtClaims.toString()));

//		// Load the key from the key store
//		KeyStore keystore = KeyStore.getInstance("JKS");
//		
//		keystore.load(new FileInputStream(jksFile), storePass.toCharArray());
//		RSAKey jwk = RSAKey.load(keystore, alias, keyPass.toCharArray());
		
		RSAKey jwk = configurationProvider.getRsaKey();

		// Get the private and public keys to sign and verify
		RSAPrivateKey privateKey = jwk.toRSAPrivateKey();

		// Create RSA-signer with the private key
		JWSSigner signer = new RSASSASigner(privateKey);

		signedJWT.sign(signer);

		return signedJWT;
	}

	@BeforeAll
	public static void init() throws SEPASecurityException {
		configurationProvider = new ConfigurationProvider();
		auth = new InMemorySecurityManager(configurationProvider.getSslContext(),configurationProvider.getRsaKey());
	}

	//@Test
	public void entitiesAuthorization() throws SEPASecurityException {
		String uid = UUID.randomUUID().toString();

		assertFalse(auth.isAuthorized("xyz"),"xyz is not authorized");

		auth.addAuthorizedIdentity(new DeviceIdentity(uid));

		assertFalse(!auth.isAuthorized(uid),"Failed to authorized");
		assertFalse(!auth.getIdentity(uid).getUid().equals(uid),"Failed to get identity");

		auth.removeAuthorizedIdentity(uid);

		assertFalse(auth.isAuthorized(uid),uid+" should not be authorized");
	}

	//@Test
	public void jwtClaims() throws SEPASecurityException {
		String issuer = auth.getIssuer();
//		String httpsAudience = auth.getHttpsAudience();
//		String wssAudience = auth.getWssAudience();
//		String subject = auth.getSubject();

		String uid = UUID.randomUUID().toString();

		auth.setIssuer(uid);
		assertFalse(!auth.getIssuer().equals(uid),"Failed to set issuer");
		
		auth.setIssuer(issuer);
//		
//		auth.setHttpsAudience(uid);
//		assertFalse("Failed to set https audience",!auth.getHttpsAudience().equals(uid));
//		
//		auth.setWssAudience(uid);
//		assertFalse("Failed to set wss audience",!auth.getWssAudience().equals(uid));
//		
//		auth.setSubject(uid);
//		assertFalse("Failed to set subject",!auth.getSubject().equals(uid));
//

//		auth.setHttpsAudience(httpsAudience);
//		auth.setWssAudience(wssAudience);
//		auth.setSubject(subject);
	}

	@Test
	public void userCredentials() throws SEPASecurityException {
		assertFalse(auth.containsCredentials("xyz"));

		String uid = UUID.randomUUID().toString();
		DigitalIdentity device = new DeviceIdentity(uid);

		auth.storeCredentials(device, uid);

		assertFalse(!auth.containsCredentials(uid),"Identity not registered");
		assertFalse(!auth.checkCredentials(uid, uid),"Failed to check password");

		auth.removeCredentials(device);
		
		assertFalse(auth.checkCredentials(uid, uid),"Identity removed: password check failed");
	}
	
	//@Test
	public void tokens() throws SEPASecurityException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, ParseException, IOException, JOSEException {	
		String uid = UUID.randomUUID().toString();
		
		DigitalIdentity device =  new DeviceIdentity(uid);
		
		auth.storeCredentials(device, uid);

		SignedJWT token = generateToken(device,uid);
		Date expirationDate = token.getJWTClaimsSet().getExpirationTime();
		
		auth.addJwt(uid,token);
		assertFalse(!auth.containsJwt(uid),"Failed to check token presence");
		assertFalse(auth.getTokenExpiringPeriod(uid) != auth.getDeviceExpiringPeriod(),"Failed to get expiring period");
		assertFalse(!auth.getTokenExpiringDate(uid).equals(expirationDate),"Failed to get expiring date");
		
		SignedJWT stored = auth.getJwt(uid);
		assertFalse(!stored.serialize().equals(token.serialize()),"Token does not match");
		
		auth.setTokenExpiringPeriod(uid,0);
		assertFalse(auth.getTokenExpiringPeriod(uid) != 0,"Failed to set expiring period");
		
	}
}
