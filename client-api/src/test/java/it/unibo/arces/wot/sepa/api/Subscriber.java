package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class Subscriber extends Thread implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private final SEPASecurityManager sm;
	private final SPARQL11SEProtocol client;
	private final String id;
	private final Sync sync;
	private String spuid;
	
	private AtomicBoolean subscribing = new AtomicBoolean(false);
	private AtomicBoolean unsubscribing = new AtomicBoolean(false);
	
	private static ConfigurationProvider provider;
	
	public Subscriber(String id,SEPASecurityManager sm, Sync sync) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		provider = new ConfigurationProvider();
		
		this.sm = sm;
		this.id = id;
		this.sync = sync;

		SubscriptionProtocol protocol;
		if (sm != null) {
			protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getDefaultHost(), provider.getJsap().getSubscribePort(),
					provider.getJsap().getSubscribePath(), sm, this);
		} else {
			protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getDefaultHost(), provider.getJsap().getSubscribePort(),
					provider.getJsap().getSubscribePath(), this);
		}

		client = new SPARQL11SEProtocol(protocol);
	}
	
	public void run() {
		try {
			subscribe();
			synchronized(this) {
				wait();
			}		
		} catch (SEPAProtocolException | InterruptedException e) {
		
		}
	}

	public void close() {
		client.close();
		interrupt();
	}

	private void subscribe() throws SEPAProtocolException, InterruptedException {
		subscribing.set(true);
		client.subscribe(provider.buildSubscribeRequest(id, 5000,sm));
	}

	public void unsubscribe(String spuid) throws SEPAProtocolException, InterruptedException {
		unsubscribing.set(true);
		client.unsubscribe(provider.buildUnsubscribeRequest(spuid, 5000,sm));
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: " + notify);

		sync.event();
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("@onError: " + errorResponse);
		if (errorResponse.isTokenExpiredError()) { 
			if (subscribing.get())
				try {
					client.subscribe(provider.buildSubscribeRequest(id, 5000,sm));
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}
			else if (unsubscribing.get())
				try {
					client.unsubscribe(provider.buildUnsubscribeRequest(spuid, 5000,sm));
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}	  
		}
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: " + spuid + " alias: " + alias);

		subscribing.set(false);
		sync.subscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe: " + spuid);

		unsubscribing.set(false);
		sync.unsubscribe();
	}
}
