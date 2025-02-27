package com.vaimee.sepa.commons.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vaimee.sepa.ConfigurationProvider;
import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.response.Response;

public class ITSEPASecurityManager {
	private static ConfigurationProvider provider;

	@BeforeAll
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		provider = new ConfigurationProvider();
	}

	@Test
	public void Register() throws SEPASecurityException, SEPAPropertiesException, InterruptedException, IOException {
		if (provider.getJsap().isSecure()) {
			OAuthProperties oauth = provider.getJsap().getAuthenticationProperties();
			ClientSecurityManager sm = new ClientSecurityManager(oauth);
			
			if (!oauth.isClientRegistered()) {
				Response ret = sm.registerClient(provider.getClientId(),oauth.getUsername(),oauth.getInitialAccessToken());
				assertFalse(ret.isError(), String.valueOf(ret));
			}
			
			sm.close();
		}
	}
	
	@Test
	public void RefreshToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException, IOException {
		if (provider.getJsap().isSecure()) {
			OAuthProperties oauth = provider.getJsap().getAuthenticationProperties();
			ClientSecurityManager sm = new ClientSecurityManager(provider.getJsap().getAuthenticationProperties());
			
			Response token = sm.refreshToken();
			
			if (token.isError()) {
				if (!oauth.isClientRegistered()) {
					Response ret = sm.registerClient(provider.getClientId(),oauth.getUsername(),oauth.getInitialAccessToken());
					assertFalse(ret.isError(), String.valueOf(ret));
				}	
				token = sm.refreshToken();
			}
			
			assertFalse(token.isError(), String.valueOf(token));

			assertFalse(oauth.getBearerAuthorizationHeader() == null, String.valueOf(token));
					
			sm.close();
		}
	}

}
