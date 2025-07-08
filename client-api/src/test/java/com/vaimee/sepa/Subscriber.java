package com.vaimee.sepa;

import java.io.Closeable;
import java.io.IOException;

import com.vaimee.sepa.api.SPARQL11SEProtocol;
import com.vaimee.sepa.api.SubscriptionProtocol;

import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.properties.SubscriptionProtocolProperties;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.logging.Logging;

public class Subscriber extends Thread implements Closeable, ISubscriptionHandler {
	private SPARQL11SEProtocol client;
	private final String id;
	private ConfigurationProvider provider;
	private ISubscriptionHandler handler;
	private String spuid = null;
	private SubscriptionProtocol protocol;

	public Subscriber(ConfigurationProvider provider, String id, ISubscriptionHandler sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {

		this.setName("Subscriber-" + id + "-" + this.threadId());
		this.provider = provider;
		this.id = id;
		this.handler = sync;
		
		SubscriptionProtocolProperties properties = provider.getJsap().getSubscribeProtocol();
		protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),properties,this,null);

		client = new SPARQL11SEProtocol(protocol);
	}

	public void run() {
		try {
			//if (sm != null) sm.refreshToken();
			client.subscribe(provider.buildSubscribeRequest(id));
			//if (sm != null) sm.close();
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException  e2) {
			Logging.logger.error(e2.getMessage());
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
					Logging.logger.error(e.getMessage());
				}
			client.notify();
			client.close();
		}

		protocol.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		Logging.logger.debug(notify);
		handler.onSemanticEvent(notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		if (errorResponse.getStatusCode() != 1000)
			Logging.logger.error(errorResponse);
		else
			Logging.logger.warn(errorResponse);
		handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.logger.error(errorResponse);
		handler.onError(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		Logging.logger.debug("onSubscribe: " + spuid + " alias: " + alias);
		this.spuid = spuid;
		handler.onSubscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		Logging.logger.debug("onUnsubscribe: " + spuid);
		handler.onUnsubscribe(spuid);
	}
}
