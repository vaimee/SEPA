package it.unibo.arces.wot.sepa;

import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationProvider {
	protected final Logger logger = LogManager.getLogger();

	private final JSAP appProfile;
	private String prefixes = "";
	private final String jsapPath; 

	private final long TIMEOUT = 5000;
	private final long NRETRY = 0;
	
	ClientSecurityManager sm = null;
	
	public ConfigurationProvider() throws SEPAPropertiesException, SEPASecurityException {
		String jsapFileName = "sepatest.jsap";
		
		if (System.getProperty("testConfiguration") != null) {
			jsapFileName = System.getProperty("testConfiguration");
			logger.info("JSAP from property testConfiguration: " + jsapFileName);
		} else if (System.getProperty("secure") != null) {
			jsapFileName = "sepatest-secure.jsap";
			logger.info("JSAP secure default: " + jsapFileName);
			
		}

		jsapPath = getClass().getClassLoader().getResource(jsapFileName).getPath();
		File f = new File(jsapPath);
		if (!f.exists()) {
			logger.error("File not found: " + jsapPath);
			throw new SEPAPropertiesException("File not found: "+jsapPath);
		}
		
		logger.info("Loading JSAP from: "+jsapPath);
		
		appProfile = new JSAP(jsapPath);
		
		prefixes = appProfile.getPrefixes();
		
		if (appProfile.isSecure()) sm = buildSecurityManager();
	}
	
	public WebsocketSubscriptionProtocol getWebsocketClient() throws SEPASecurityException, SEPAProtocolException {
		return new WebsocketSubscriptionProtocol(appProfile.getHost(), appProfile.getSubscribePort(), appProfile.getSubscribePath(), null, sm);
	}

	private String getSPARQLUpdate(String id) {
		return prefixes + " " +appProfile.getSPARQLUpdate(id);
	}
	
	private String getSPARQLQuery(String id) {
		return prefixes + " " +appProfile.getSPARQLQuery(id);
	}
	
	public UpdateRequest buildUpdateRequest(String id) { //, ClientSecurityManager sm,long timeout,long nRetry) {
		String authorization = null;

		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
				if (authorization == null) {
					sm.refreshToken();
					authorization = sm.getAuthorizationHeader();
				}
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}

		return new UpdateRequest(appProfile.getUpdateMethod(id), appProfile.getUpdateProtocolScheme(id),
				appProfile.getUpdateHost(id), appProfile.getUpdatePort(id), appProfile.getUpdatePath(id),
				getSPARQLUpdate(id), appProfile.getUsingGraphURI(id), appProfile.getUsingNamedGraphURI(id),
				authorization, TIMEOUT,NRETRY);
	}

	public QueryRequest buildQueryRequest(String id) {//, ClientSecurityManager sm,long timeout,long nRetry) {
		String authorization = null;

		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
				if (authorization == null) {
					sm.refreshToken();
					authorization = sm.getAuthorizationHeader();
				}
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}

		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id),
				authorization, TIMEOUT,NRETRY);
	}

	public QueryRequest buildQueryRequest(String id, String authToken) {
		return new QueryRequest(appProfile.getQueryMethod(id), appProfile.getQueryProtocolScheme(id),
				appProfile.getQueryHost(id), appProfile.getQueryPort(id), appProfile.getQueryPath(id),
				getSPARQLQuery(id), appProfile.getDefaultGraphURI(id), appProfile.getNamedGraphURI(id),
				authToken, TIMEOUT,NRETRY);
	}

	public SubscribeRequest buildSubscribeRequest(String id) { //), ClientSecurityManager sm) {
		String authorization = null;		
		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
				if (authorization == null) {
					sm.refreshToken();
					authorization = sm.getAuthorizationHeader();
				}
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		
		return new SubscribeRequest(getSPARQLQuery(id), id, appProfile.getDefaultGraphURI(id),
				appProfile.getNamedGraphURI(id), authorization);
	}

	public UnsubscribeRequest buildUnsubscribeRequest(String spuid) { //, ClientSecurityManager sm) {
		String authorization = null;		
		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
				if (authorization == null) {
					sm.refreshToken();
					authorization = sm.getAuthorizationHeader();
				}
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		
		return new UnsubscribeRequest(spuid, authorization);
	}

	public ClientSecurityManager getSecurityManager() {
		return sm;
	}
	
	private ClientSecurityManager buildSecurityManager() throws SEPASecurityException, SEPAPropertiesException {
		AuthenticationProperties auth = new AuthenticationProperties(jsapPath);
		
		ClientSecurityManager security;
		if (auth.trustAll()) security = new ClientSecurityManager(auth);
		else {
			String path = getClass().getClassLoader().getResource("sepa.jks").getPath();
			File f = new File(path);
			if (!f.exists()) {
				logger.error("File not found: " + path);
				throw new SEPASecurityException("File not found: "+path);
			}
			
			security = new ClientSecurityManager(auth,f.getPath(), "sepa2017");
		}
		
		return security;
	}
	
	public JSAP getJsap() {
		return appProfile;
	}
}
