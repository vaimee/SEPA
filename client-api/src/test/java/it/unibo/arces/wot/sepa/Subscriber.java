package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class Subscriber extends Thread implements Closeable, ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private SPARQL11SEProtocol client;
	private final String id;
	private ConfigurationProvider provider;
	private ISubscriptionHandler handler;
	private String spuid = null;
	private SubscriptionProtocol protocol;

	public Subscriber(ConfigurationProvider provider, String id, ISubscriptionHandler sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {

		this.setName("Subscriber-" + id + "-" + this.getId());
		this.provider = provider;
		this.id = id;
		this.handler = sync;
		
		protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(), this, provider.getClientSecurityManager());

		client = new SPARQL11SEProtocol(protocol);
	}

	public void run() {
		try {
			//if (sm != null) sm.refreshToken();
			client.subscribe(provider.buildSubscribeRequest(id));
			//if (sm != null) sm.close();
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException  e2) {
			logger.error(e2.getMessage());
			return;
		}

		synchronized(client) {
			try {
				client.wait();
			} catch (InterruptedException e) {
				
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized(client) {
			if (spuid != null)
				try {
					client.unsubscribe(provider.buildUnsubscribeRequest(spuid));
				} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
					logger.error(e.getMessage());
				}
			client.notify();
			client.close();
		}

		protocol.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug(notify);
		handler.onSemanticEvent(notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		if (errorResponse.getStatusCode() != 1000)
			logger.error(errorResponse);
		else
			logger.warn(errorResponse);
		handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
		handler.onError(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("onSubscribe: " + spuid + " alias: " + alias);
		this.spuid = spuid;
		handler.onSubscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("onUnsubscribe: " + spuid);
		handler.onUnsubscribe(spuid);
	}
}
