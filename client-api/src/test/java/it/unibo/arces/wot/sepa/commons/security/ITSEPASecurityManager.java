package it.unibo.arces.wot.sepa.commons.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ITSEPASecurityManager {
	private static ClientSecurityManager sm = null;
	private static JSAP app = null;
	
	private String testId = "SEPATest";
	private String notAllowedId = "IamNotAllowedToRegister";
	
	@BeforeAll
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		ConfigurationProvider provider = new ConfigurationProvider();
		
		app = provider.getJsap();
		
		sm = provider.getSecurityManager();
	}

	@Test
	//(timeout = 2000)
	public void Register() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException  {
		if (sm == null && !app.isSecure()) return;
		
		Response ret = sm.register(testId);
		assertFalse(ret.isError(),String.valueOf(ret));
		
		ret = sm.register(notAllowedId);
		assertFalse(!ret.isError(),String.valueOf(ret));
	}
	
	@Test
	//(timeout = 15000)
	public void GetAuthorizationHeader() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (sm == null && !app.isSecure()) return;
		
		Response ret = sm.register(testId);
		assertFalse(ret.isError(),String.valueOf(ret));
		
		// In test conditions token expires in 5 seconds
		for (int i=0; i < 10; i++) {
			sm.getAuthorizationHeader();
			Thread.sleep(1000);
		}
	}
	
}
