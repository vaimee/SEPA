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
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationProvider {
	protected final Logger logger = LogManager.getLogger();

	private final JSAP appProfile;
	private String prefixes = "";
	private final String jsapPath;

	public final long TIMEOUT;
	public final long NRETRY;
	public static final int REPEATED_TEST = 1;

	public ConfigurationProvider() throws SEPAPropertiesException, SEPASecurityException {
		String jsapFileName = "sepatest.jsap";

		if (System.getProperty("testConfiguration") != null) {
			jsapFileName = System.getProperty("testConfiguration");
			logger.debug("JSAP from property testConfiguration: " + jsapFileName);
		} else if (System.getProperty("secure") != null) {
			jsapFileName = "sepatest-secure.jsap";
			logger.debug("JSAP secure default: " + jsapFileName);
		}

		jsapPath = getClass().getClassLoader().getResource(jsapFileName).getPath();
		File f = new File(jsapPath);
		if (!f.exists()) {
			logger.error("File not found: " + jsapPath);
			throw new SEPAPropertiesException("File not found: " + jsapPath);
		}

		logger.debug("Loading JSAP from: " + jsapPath);

		appProfile = new JSAP(jsapPath);

		prefixes = appProfile.getPrefixes();

		if (appProfile.getExtendedData().has("timeout")) {
			TIMEOUT = appProfile.getExtendedData().get("timeout").getAsLong();
		} else
			TIMEOUT = 15000;

		if (appProfile.getExtendedData().has("nretry")) {
			NRETRY = appProfile.getExtendedData().get("nretry").getAsLong();
		} else
			NRETRY = 3;
	}

	private String getSPARQLUpdate(String id) {
		return prefixes + " " + appProfile.getSPARQLUpdate(id);
	}

	private String getSPARQLQuery(String id) {
		return prefixes + " " + appProfile.getSPARQLQuery(id);
	}

	public UpdateRequest buildUpdateRequest(String id, ClientSecurityManager sm)
			throws SEPASecurityException, SEPAPropertiesException { // , ClientSecurityManager sm,long timeout,long
																	// nRetry) {
		return new UpdateRequest(appProfile.getUpdateMethod(id), appProfile.getUpdateProtocolScheme(id),
				appProfile.getUpdateHost(id), appProfile.getUpdatePort(id), appProfile.getUpdatePath(id),
				getSPARQLUpdate(id), appProfile.getUsingGraphURI(id), appProfile.getUsingNamedGraphURI(id),
				(sm == null ? null : sm.getAuthorizationHeader()), TIMEOUT, NRETRY);
	}

	public QueryRequest buildQueryRequest(String id, ClientSecurityManager sm)
			throws SEPASecurityException, SEPAPropertiesException {// , ClientSecurityManager sm,long timeout,long
																	// nRetry) {
		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id),
				(sm == null ? null : sm.getAuthorizationHeader()), TIMEOUT, NRETRY);
	}

	public QueryRequest buildQueryRequest(String id, String authToken) {
		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id), authToken,
				TIMEOUT, NRETRY);
	}

	public SubscribeRequest buildSubscribeRequest(String id, ClientSecurityManager sm)
			throws SEPASecurityException, SEPAPropertiesException { // ), ClientSecurityManager sm) {
		return new SubscribeRequest(getSPARQLQuery(id), id, appProfile.getDefaultGraphURI(id),
				appProfile.getNamedGraphURI(id), (sm == null ? null : sm.getAuthorizationHeader()));
	}

	public UnsubscribeRequest buildUnsubscribeRequest(String spuid, ClientSecurityManager sm)
			throws SEPASecurityException, SEPAPropertiesException {
		return new UnsubscribeRequest(spuid, (sm == null ? null : sm.getAuthorizationHeader()));
	}

	public JSAP getJsap() {
		return appProfile;
	}

	public ClientSecurityManager buildSecurityManager() throws SEPASecurityException, SEPAPropertiesException {
		ClientSecurityManager sm = null;
		if (appProfile.isSecure()) {
			OAuthProperties oauth = new OAuthProperties(jsapPath);
			sm = new ClientSecurityManager(oauth);

			if (!sm.isClientRegistered()) {
				Response ret = sm.registerClient(getClientId(),oauth.getUsername(),oauth.getInitialAccessToken());
				if (ret.isError())
					throw new SEPASecurityException(getClientId() + " registration failed");
				sm.storeOAuthProperties();
			}
			sm.refreshToken();
		}

		return sm;
	}

	public String getClientId() throws SEPAPropertiesException, SEPASecurityException {
		if (appProfile.isSecure()) {
			OAuthProperties oauth = new OAuthProperties(appProfile.getFileName());
			if (oauth.getProvider().equals(OAUTH_PROVIDER.SEPA))
				return "SEPATest";
			else if (oauth.getProvider().equals(OAUTH_PROVIDER.KEYCLOAK))
				return "SEPATest-" + UUID.randomUUID().toString();
		}

		return null;
	}
}
