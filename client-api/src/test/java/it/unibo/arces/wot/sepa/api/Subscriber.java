package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

class Subscriber extends Thread implements ISubscriptionHandler, Closeable {
	protected final Logger logger = LogManager.getLogger();

	private final ClientSecurityManager sm;
	private final SPARQL11SEProtocol client;
	private final String id;
	private final Sync sync;

	private static ConfigurationProvider provider;

	public Subscriber(String id, Sync sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		this.setName("Subscriber-"+id+"-"+this.getId());
		provider = new ConfigurationProvider();
		
		this.id = id;
		this.sync = sync;
		
		if (provider.getJsap().isSecure()) sm = provider.buildSecurityManager();
		else sm = null;
		
		SubscriptionProtocol protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
					provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(),this,sm);

		client = new SPARQL11SEProtocol(protocol);
	}

	public void run() {
		if(provider.getJsap().isSecure()){
			try {
				sm.register("SEPATest");
			} catch (SEPASecurityException | SEPAPropertiesException  e) {
				logger.error(e);
			}
		}
		synchronized (this) {
			try {
				subscribe();
				wait();
			} catch (SEPAProtocolException | InterruptedException e) {
				try {
					client.close();
				} catch (IOException e1) {
					logger.error(e1.getMessage());
				}
				return;
			}
		}
	}

	public void close() {	
		interrupt();
	}

	private void subscribe() throws SEPAProtocolException, InterruptedException {
		client.subscribe(provider.buildSubscribeRequest(id, 5000, sm));
	}

	public void unsubscribe(String spuid) throws SEPAProtocolException, InterruptedException {
		client.unsubscribe(provider.buildUnsubscribeRequest(spuid, 5000, sm));
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: " + notify);

		if (sync != null)
			sync.event();
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		if (errorResponse.isTokenExpiredError()) {
			// Expired token
			try {
				logger.error("Token expired. Refreshing token and subscribe again");
				sm.refreshToken();
				subscribe();
			} catch (SEPAProtocolException | InterruptedException | SEPAPropertiesException | SEPASecurityException e) {
				logger.error("Failed to subscribe after token expired "+e.getMessage());
			}
		}
		else {
			logger.error("@onError: " + errorResponse);
		}
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: " + spuid + " alias: " + alias);

		if (sync != null)
			sync.subscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe: " + spuid);

		if (sync != null)
			sync.unsubscribe();
	}
}
