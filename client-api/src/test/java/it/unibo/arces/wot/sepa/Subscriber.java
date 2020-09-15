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

public class Subscriber extends Thread implements Closeable,ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private final SPARQL11SEProtocol client;
	private final String id;
	private ConfigurationProvider provider;
	private ISubscriptionHandler handler;
	private String spuid = null;
	
	public Subscriber(ConfigurationProvider provider,String id, ISubscriptionHandler sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {

		this.setName("Subscriber-" + id + "-" + this.getId());
		this.provider = provider;
		this.id = id;
		this.handler = sync;

		SubscriptionProtocol protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(), this,
				provider.getSecurityManager());

		this.client = new SPARQL11SEProtocol(protocol);
	}

	public void run() {
		synchronized (this) {
			try {
				logger.debug("subscribe");
				client.subscribe(provider.buildSubscribeRequest(id));

				logger.debug("wait");
				wait();
			} catch (SEPAProtocolException | InterruptedException | SEPASecurityException | SEPAPropertiesException e) {

			}
		}
	}

	public void close() {
		logger.debug("close");
		try {
			if (spuid != null) client.unsubscribe(provider.buildUnsubscribeRequest(spuid));
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		interrupt();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug(notify);
		handler.onSemanticEvent(notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		logger.error(errorResponse);
		handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
		if (errorResponse.isTokenExpiredError())
			try {
				provider.getSecurityManager().refreshToken();
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				logger.error("Failed to refresh token." +e.getMessage());
			}
		handler.onError(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("onSubscribe: "+spuid+" alias: "+alias);
		this.spuid = spuid;
		handler.onSubscribe(spuid, alias);	
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("onUnsubscribe: "+spuid);
		handler.onUnsubscribe(spuid);
	}
}
