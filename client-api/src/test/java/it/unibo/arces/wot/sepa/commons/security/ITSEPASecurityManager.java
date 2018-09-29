package it.unibo.arces.wot.sepa.commons.security;

import it.unibo.arces.wot.sepa.api.ITSPARQL11SEProtocol;
import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class ITSEPASecurityManager {
	static {
		ConfigurationProvider.configureLogger();
	}
	private static SEPASecurityManager sm = null;
	private static JSAP app = null;
	
	private String testId = "SEPATest";
	private String notAllowedId = "IamNotAllowedToRegister";
	
	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		app = new ConfigurationProvider().getJsap();
		
		if (app.isSecure()){
			ClassLoader classLoader = ITSPARQL11SEProtocol.class.getClassLoader();
			File keyFile = new File(classLoader.getResource("sepa.jks").getFile());
			sm = new SEPASecurityManager(keyFile.getPath(), "sepa2017", "sepa2017",
					app.getAuthenticationProperties());
		}
	}

	@Test(timeout = 2000)
	public void Register() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException  {
		if (sm == null && !app.isSecure()) return;
		
		Response ret = sm.register(testId);
		assertFalse(String.valueOf(ret),ret.isError());
		
		ret = sm.register(notAllowedId);
		assertFalse(String.valueOf(ret),!ret.isError());
	}
	
	@Test(timeout = 15000)
	public void GetAuthorizationHeader() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (sm == null && !app.isSecure()) return;
		
		Response ret = sm.register(testId);
		assertFalse(String.valueOf(ret),ret.isError());
		
		// In test conditions token expires in 5 seconds
		for (int i=0; i < 10; i++) {
			sm.getAuthorizationHeader();
			Thread.sleep(1000);
		}
	}
	
}
