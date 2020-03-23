package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

class Subscriber extends Thread implements Closeable {
	private final WebsocketSubscriptionProtocol client;
	private int n;

	private static ConfigurationProvider provider;

	protected final Logger logger = LogManager.getLogger();
	protected ClientSecurityManager sm = null;

	public Subscriber(int n, ISubscriptionHandler handler) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		provider = new ConfigurationProvider();

		if (provider.getJsap().isSecure())
			sm = provider.buildSecurityManager();
		else
			sm = null;
		
		client = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(), handler,sm);
		
		this.n = n;
	}

	public void run() {
		if (provider.getJsap().isSecure()) {
			try {
				sm.register("SEPATest");
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e);
			}
		}
		for (int j = 0; j < n; j++) {
			try {
				client.subscribe(provider.buildSubscribeRequest("RANDOM", 500, sm));
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

	public void unsubscribe(String spuid) throws SEPAProtocolException {
		client.unsubscribe(provider.buildUnsubscribeRequest(spuid, 500, sm));
	}

	@Override
	public void close() throws IOException {
		n = 0;
		interrupt();
	}
}
