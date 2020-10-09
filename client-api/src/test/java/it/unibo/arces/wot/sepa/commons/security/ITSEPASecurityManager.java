package it.unibo.arces.wot.sepa.commons.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

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
	public void Register() throws SEPASecurityException, SEPAPropertiesException, InterruptedException, IOException {
		if (provider.getJsap().isSecure()) {
			ClientSecurityManager sm = new ClientSecurityManager(provider.getJsap().getAuthenticationProperties(), "sepa.jks", "sepa2020");
			
			if (!sm.isClientRegistered()) {
				Response ret = sm.register(provider.getClientId());
				assertFalse(ret.isError(), String.valueOf(ret));
			}
			
			sm.storeOAuthProperties();
			sm.close();
		}
	}
	
	@Test
	public void RefreshToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException, IOException {
		if (provider.getJsap().isSecure()) {
			ClientSecurityManager sm = new ClientSecurityManager(provider.getJsap().getAuthenticationProperties(), "sepa.jks", "sepa2020");
			Response token = sm.refreshToken();
			if (token.isError()) {
				if (!sm.isClientRegistered()) {
					Response ret = sm.register(provider.getClientId());
					assertFalse(ret.isError(), String.valueOf(ret));
					sm.storeOAuthProperties();
				}	
				token = sm.refreshToken();
			}
			
			assertFalse(token.isError(), String.valueOf(token));

			assertFalse(sm.getAuthorizationHeader() == null, String.valueOf(token));
			
			sm.storeOAuthProperties();			
			sm.close();
		}
	}

}
