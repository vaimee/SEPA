package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import java.io.File;

import javax.net.ssl.SSLContext;

import com.nimbusds.jose.jwk.RSAKey;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.logging.Logging;

public class ConfigurationProvider {
	
	private final String keyPass ="sepa2020";
	private final String storePass ="sepa2020";
	private final String alias ="jwt";
	private final String jks ="sepa.jks";
	private final SSLContext ssl;
	private final RSAKey key;
	
	public ConfigurationProvider() throws SEPASecurityException {
		File jksFile = new File(getClass().getClassLoader().getResource(jks).getFile());
		ssl = JKSUtil.getSSLContext(jksFile.getPath(),storePass);
		key = JKSUtil.getRSAKey(jksFile.getPath(),storePass,alias,keyPass);
		
		Logging.logger.debug("JKS: "+jksFile.getAbsolutePath());
	}
	
	public SSLContext getSslContext() {
		return ssl;
	}
	
	public RSAKey getRsaKey() {
		return key;
	}
}
