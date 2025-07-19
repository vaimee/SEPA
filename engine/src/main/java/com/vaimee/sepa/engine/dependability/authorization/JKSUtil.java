package com.vaimee.sepa.engine.dependability.authorization;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.security.SSLManager;
import com.vaimee.sepa.logging.Logging;

public class JKSUtil {
	
	public static SSLContext getSSLContext(String keystore,String storepass) throws SEPASecurityException {
		SSLContext ctx = new SSLManager().getSSLContextFromJKS(keystore, storepass);
		return ctx;
	}
	
	public static RSAKey getRSAKey(String keystore,String storepass,String keyalias,String keypass) throws SEPASecurityException {
		KeyStore jks;
		try {
			jks = KeyStore.getInstance("JKS");
			jks.load(new FileInputStream(keystore), storepass.toCharArray());
			Logging.getLogger().debug(jks);
			return RSAKey.load(jks, keyalias, keypass.toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | JOSEException e) {
			throw new SEPASecurityException(e.getMessage());
		}		
	}
}
