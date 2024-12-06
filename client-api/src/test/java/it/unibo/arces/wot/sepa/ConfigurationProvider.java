package it.unibo.arces.wot.sepa;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.OAuthProperties;
import it.unibo.arces.wot.sepa.commons.security.OAuthProperties.OAUTH_PROVIDER;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import java.io.Closeable;
//import java.io.File;
import java.io.IOException;
//import java.net.URI;
import java.util.UUID;

public class ConfigurationProvider implements Closeable {
	private final JSAP appProfile;
	private String prefixes = "";
	//private final String jsapPath;

	public long TIMEOUT;
	public long NRETRY;
	public static final int REPEATED_TEST = 1;
	public static final long SLEEP = 0;
	
	protected ClientSecurityManager sm = null;
	
	public ConfigurationProvider() throws SEPAPropertiesException, SEPASecurityException {
		String jsapFileName = "sepatest.jsap";

//		if (System.getProperty("testConfiguration") != null) {
//			jsapFileName = System.getProperty("testConfiguration");
//			Logging.logger.debug("JSAP from property testConfiguration: " + jsapFileName);
//		} else if (System.getProperty("secure") != null) {
//			jsapFileName = "sepatest-secure.jsap";
//			Logging.logger.debug("JSAP secure default: " + jsapFileName);
//		}
//
//		jsapPath = getClass().getClassLoader().getResource(jsapFileName).getPath();
//		File f = new File(jsapPath);
//		if (!f.exists()) {
//			Logging.logger.error("File not found: " + jsapPath);
//			throw new SEPAPropertiesException("File not found: " + jsapPath);
//		}
//
//		Logging.logger.debug("Loading JSAP from: " + f.getPath());

		try {
//			appProfile = new JSAP(URI.create(f.getPath()));
			appProfile = new JSAP(jsapFileName);
		} catch (SEPAPropertiesException e) {
			Logging.logger.error(e.getMessage());
			throw new RuntimeException(e);
		}

		prefixes = "";
		for(String pre : appProfile.getPrefixes().getNsPrefixMap().keySet()) {
			prefixes += "PREFIX "+pre+":<"+appProfile.getPrefixes().getNsPrefixURI(pre)+"> ";
		}
		

		try {
			TIMEOUT = appProfile.getExtendedData().get("timeout").getAsLong();
		}
		catch(Exception e) {
			Logging.logger.warn("Extended-timeout NOT FOUND. Use default 15 s");
			TIMEOUT = 15000;
		}
		
		try {
			NRETRY = appProfile.getExtendedData().get("nretry").getAsLong();
		}
		catch(Exception e) {
			Logging.logger.warn("Extended-nretry NOT FOUND. Use default 3");
			NRETRY = 3;
			
		}

		sm = buildSecurityManager();
		
		Logging.logger.debug("Loaded JSAP: " + appProfile);
	}

	private String getSPARQLUpdate(String id) {
		return prefixes + " " + appProfile.getSPARQLUpdate(id);
	}

	private String getSPARQLQuery(String id) {
		return prefixes + " " + appProfile.getSPARQLQuery(id);
	}

	public UpdateRequest buildUpdateRequest(String id)
			throws SEPASecurityException, SEPAPropertiesException {
		return new UpdateRequest(appProfile.getUpdateMethod(id), appProfile.getUpdateProtocolScheme(id),
				appProfile.getUpdateHost(id), appProfile.getUpdatePort(id), appProfile.getUpdatePath(id),
				getSPARQLUpdate(id), appProfile.getUsingGraphURI(id), appProfile.getUsingNamedGraphURI(id),
				(appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null), TIMEOUT, NRETRY);
	}

	public QueryRequest buildQueryRequest(String id)
			throws SEPASecurityException, SEPAPropertiesException {
		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id),
				(appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null), TIMEOUT, NRETRY);
	}

	public QueryRequest buildQueryRequest(String id, String authToken) {
		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id), authToken,
				TIMEOUT, NRETRY);
	}

	public SubscribeRequest buildSubscribeRequest(String id)
			throws SEPASecurityException, SEPAPropertiesException {
		return new SubscribeRequest(getSPARQLQuery(id), id, appProfile.getDefaultGraphURI(id),
				appProfile.getNamedGraphURI(id), (appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null));
	}

	public UnsubscribeRequest buildUnsubscribeRequest(String spuid)
			throws SEPASecurityException, SEPAPropertiesException {
		return new UnsubscribeRequest(spuid, (appProfile.isSecure() ? appProfile.getAuthenticationProperties().getBearerAuthorizationHeader() : null));
	}

	public JSAP getJsap() {
		return appProfile;
	}
	
	public ClientSecurityManager getClientSecurityManager() {
		return sm;
	}

	private ClientSecurityManager buildSecurityManager() throws SEPASecurityException, SEPAPropertiesException {
		ClientSecurityManager sm = null;
		
		if (appProfile.isSecure()) {
			sm = new ClientSecurityManager(appProfile.getAuthenticationProperties());

			if (!appProfile.getAuthenticationProperties().isClientRegistered()) {
				Response ret = sm.registerClient(getClientId(),appProfile.getAuthenticationProperties().getUsername(),appProfile.getAuthenticationProperties().getInitialAccessToken());
				if (ret.isError())
					throw new SEPASecurityException(getClientId() + " registration failed");
			}
			sm.refreshToken();
			appProfile.getAuthenticationProperties().storeProperties();
		}

		return sm;
	}

	public String getClientId() throws SEPAPropertiesException, SEPASecurityException {
		if (appProfile.isSecure()) {
			OAuthProperties oauth = new OAuthProperties(appProfile);
			if (oauth.getProvider().equals(OAUTH_PROVIDER.SEPA))
				return "SEPATest";
			else if (oauth.getProvider().equals(OAUTH_PROVIDER.KEYCLOAK))
				return "SEPATest-" + UUID.randomUUID().toString();
		}

		return null;
	}

	@Override
	public void close() throws IOException {
		if (sm != null) sm.close();		
	}
}
