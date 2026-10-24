package com.vaimee.sepa.apps.chat;

import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.security.ClientSecurityManager;
import com.vaimee.sepa.api.pattern.JSAP;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JSAPProvider {
	private static final Logger logger = LogManager.getLogger();

	private final JSAP appProfile;
	
	private final long timeout = 5000;
	private final long nRetry = 0;

	public JSAPProvider() throws SEPAPropertiesException, SEPASecurityException {
		String jsapFileName = "chat.jsap";

		if (System.getProperty("testConfiguration") != null) {
			jsapFileName = System.getProperty("testConfiguration");
			logger.info("JSAP from property testConfiguration: " + jsapFileName);
		}

		// Secure engine (Zitadel security and ACL): every request carries this bearer token
		String token = System.getProperty("sepaToken", System.getenv("SEPA_TOKEN"));
		if (token != null && !token.isBlank()) {
			logger.info("Requests authorized with the bearer token from SEPA_TOKEN / -DsepaToken");
			appProfile = new BearerTokenJSAP(jsapFileName, token.trim());
		} else {
			appProfile = new JSAP(jsapFileName);
		}
	}
	
	public ClientSecurityManager getSecurityManager() throws SEPASecurityException {
		if (!appProfile.isSecure()) return null;
		
		String path = getClass().getClassLoader().getResource("sepa.jks").getPath();
		File f = new File(path);
		if (!f.exists()) {
			logger.error("File not found: " + path);
			throw new SEPASecurityException("File not found: "+path);
		}
		return new ClientSecurityManager(appProfile.getAuthenticationProperties());
	}
	
	public JSAP getJsap() {
		return appProfile;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	public long getNRetry() {
		return nRetry;
	}
}
