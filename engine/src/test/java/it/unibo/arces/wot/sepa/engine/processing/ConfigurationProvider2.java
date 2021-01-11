package it.unibo.arces.wot.sepa.engine.processing;

import java.io.File;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.jwk.RSAKey;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class ConfigurationProvider2 {
	protected final Logger logger = LogManager.getLogger();
	public String a = "";
	
	public ConfigurationProvider2() throws SEPASecurityException {
		File jksFile = new File(getClass().getClassLoader().getResource("endpoint.jpar").getFile());
		a=jksFile.getAbsolutePath();
	}
	
	
}
