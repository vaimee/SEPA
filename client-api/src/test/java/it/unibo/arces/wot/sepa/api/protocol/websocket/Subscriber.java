package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

class Subscriber extends Thread implements Closeable,ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();
	
	private final WebsocketSubscriptionProtocol client;
	private int n;

	private static ConfigurationProvider provider;
		
	private ISubscriptionHandler handler;
	
	private HashSet<String> spuids = new HashSet<>();
	
	public Subscriber(int n, ISubscriptionHandler handler) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		provider = new ConfigurationProvider();
		
		this.handler = handler;
		
		client = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(), this,provider.getSecurityManager());
		
		this.n = n;
	}

	public void run() {
		if (provider.getJsap().isSecure()) {
			try {
				provider.getSecurityManager().register("SEPATest");
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e);
			}
		}
		
		for (int j = 0; j < n; j++) {
			try {
				client.subscribe(provider.buildSubscribeRequest("RANDOM"));
			} catch (SEPAProtocolException e) {
				logger.error(e.getMessage());
			}
		}

		try {
			synchronized (this) {
				wait();
			}
		} catch (InterruptedException e1) {

		}
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public void unsubscribe() throws SEPAProtocolException {
		for (String spuid : spuids) client.unsubscribe(provider.buildUnsubscribeRequest(spuid));
	}

	@Override
	public void close() throws IOException {
		HashSet<String> temp = new HashSet<>();
		for (String spuid : spuids) temp.add(spuid);
		for (String spuid : temp) 
			try {
				client.unsubscribe(provider.buildUnsubscribeRequest(spuid));
			} catch (SEPAProtocolException e) {
				logger.error(e.getMessage());
			}
		interrupt();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		handler.onSemanticEvent(notify);	
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		handler.onError(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		spuids.add(spuid);
		handler.onSubscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		spuids.remove(spuid);
		handler.onUnsubscribe(spuid);
	}
}
