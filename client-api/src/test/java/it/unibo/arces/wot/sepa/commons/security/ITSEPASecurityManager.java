package it.unibo.arces.wot.sepa.commons.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class ITSEPASecurityManager {
	private static ConfigurationProvider provider;

	@BeforeAll
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		provider = new ConfigurationProvider();
	}

	@Test
	public void Register() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (provider.getSecurityManager() == null && !provider.getJsap().isSecure())
			return;
		
		Response ret = provider.getSecurityManager().register(provider.getClientId());

		assertFalse(ret.isError(), String.valueOf(ret));
		
		provider.getSecurityManager().storeOAuthProperties();
	}
	
	@Test
	public void RefreshToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (provider.getSecurityManager() == null && !provider.getJsap().isSecure())
			return;
		
		Response token = provider.getSecurityManager().refreshToken();
		
		if (token.isError()) {
			Response ret = provider.getSecurityManager().register(provider.getClientId());

			assertFalse(ret.isError(), String.valueOf(ret));	
		
			token = provider.getSecurityManager().refreshToken();
		}
		
		assertFalse(token.isError(), String.valueOf(token));

		assertFalse(provider.getSecurityManager().getAuthorizationHeader() == null, String.valueOf(token));
		
		provider.getSecurityManager().storeOAuthProperties();
	}

}
