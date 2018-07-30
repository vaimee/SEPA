package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Subscriber implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private final JSAP properties;
	private final SEPASecurityManager sm;
	private final SPARQL11SEProtocol client;
	private final String id;
	private final Sync sync;
	private String spuid;
	
	private AtomicBoolean subscribing = new AtomicBoolean(false);
	private AtomicBoolean unsubscribing = new AtomicBoolean(false);
	
	
	public Subscriber(String id, JSAP properties, SEPASecurityManager sm, Sync sync) throws SEPAProtocolException, SEPASecurityException {
		this.properties = properties;
		this.sm = sm;
		this.id = id;
		this.sync = sync;

		SubscriptionProtocol protocol;
		if (sm != null) {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), sm, this);
		} else {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), this);
		}

		client = new SPARQL11SEProtocol(protocol);
	}

	public void close() {
		client.close();
	}

	public void subscribe() throws SEPAProtocolException, InterruptedException {
		subscribing.set(true);
		client.subscribe(buildSubscribeRequest(id, 5000));
	}

	public void unsubscribe(String spuid) throws SEPAProtocolException, InterruptedException {
		unsubscribing.set(true);
		client.unsubscribe(buildUnsubscribeRequest(spuid, 5000));
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
					client.subscribe(buildSubscribeRequest(id, 5000));
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}
			else if (unsubscribing.get())
				try {
					client.unsubscribe(buildUnsubscribeRequest(spuid, 5000));
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

	private SubscribeRequest buildSubscribeRequest(String id, long timeout) {
		String authorization = null;		
		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		
		return new SubscribeRequest(properties.getSPARQLQuery(id), id, properties.getDefaultGraphURI(id),
				properties.getNamedGraphURI(id), authorization, timeout);
	}

	private UnsubscribeRequest buildUnsubscribeRequest(String spuid, long timeout) {
		String authorization = null;		
		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		
		return new UnsubscribeRequest(spuid, authorization, timeout);
	}
}
