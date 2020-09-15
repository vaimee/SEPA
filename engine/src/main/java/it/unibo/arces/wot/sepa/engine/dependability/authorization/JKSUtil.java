package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SSLManager;

public class JKSUtil {
	protected static Logger logger = LogManager.getLogger();
	
	public static SSLContext getSSLContext(String keystore,String storepass) throws SEPASecurityException {
		SSLContext ctx = new SSLManager().getSSLContextFromJKS(keystore, storepass);
		return ctx;
	}
	
	public static RSAKey getRSAKey(String keystore,String storepass,String keyalias,String keypass) throws SEPASecurityException {
		KeyStore jks;
		try {
			jks = KeyStore.getInstance("JKS");
			jks.load(new FileInputStream(keystore), storepass.toCharArray());
			logger.debug(jks);
			return RSAKey.load(jks, keyalias, keypass.toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | JOSEException e) {
			throw new SEPASecurityException(e.getMessage());
		}		
	}
}
