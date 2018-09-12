package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.api.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
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

import static org.junit.Assert.*;

public class ITWebSocketSubscriptionProtocol implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private static JSAP app = null;
	private WebsocketSubscriptionProtocol client = null;
	private static SEPASecurityManager sm = null;
	private static AtomicLong subscribes = new AtomicLong(0);
	private String spuid = null;

	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException {
		app = ConfigurationProvider.GetTestEnvConfiguration();

		if (app.isSecure()) {
			sm = new SEPASecurityManager(app.getAuthenticationProperties());
			sm.register("SEPATest");
		}
	}

	@Before
	public void before()
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, URISyntaxException {
		if (app.isSecure()) {
			client = new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
					app.getSubscribePath(), sm, this);
		} else
			client = new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
					app.getSubscribePath(), this);

		subscribes.set(0);
	}

	@After
	public void after() {

	}

	@Test(timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), sm.getAuthorizationHeader(), 5000);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), null, 5000);

		logger.debug(request);

		client.subscribe(request);

		while (subscribes.get() != 1) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		client.close();

		assertFalse("Failed to subscribe", subscribes.get() != 1);
	}

	@Test(timeout = 5000)
	public void SubscribexN() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		int n = 500;

		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), sm.getAuthorizationHeader(), 500);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), null, 500);

		for (int i = 0; i < n; i++) {
			logger.debug(request);
			client.subscribe(request);
		}

		while (subscribes.get() < n) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to subscribe", subscribes.get() != n);

		client.close();
	}

	@Test(timeout = 10000)
	public void SubscribeMxN() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		int n = 50;
		int m = 20;

		ArrayList<WebsocketSubscriptionProtocol> clients = new ArrayList<WebsocketSubscriptionProtocol>();

		for (int i = 0; i < m; i++) {
			if (app.isSecure()) {
				clients.add(new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
						app.getSubscribePath(), sm, this));
			} else
				clients.add(new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
						app.getSubscribePath(), this));
		}

		for (int i = 0; i < m; i++) {
			new Subscriber(clients.get(i),n).start();
		}

		while (subscribes.get() < n * m) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to subscribe", subscribes.get() != n * m);

		client.close();
	}

	class Subscriber extends Thread {
		private WebsocketSubscriptionProtocol client;
		private int n;

		public Subscriber(WebsocketSubscriptionProtocol client, int n) {
			this.client = client;
			this.n = n;
		}

		public void run() {
			for (int j = 0; j < n; j++) {
				SubscribeRequest request = null;
				if (app.isSecure()) {
					try {
						request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
								app.getNamedGraphURI("RANDOM"), sm.getAuthorizationHeader(), 500);
					} catch (SEPASecurityException | SEPAPropertiesException e) {
						logger.error(e.getMessage());
					}
				} else
					request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
							app.getNamedGraphURI("RANDOM"), null, 500);
				logger.debug(request);
				try {
					client.subscribe(request);
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Test(timeout = 5000)
	public void SubscribeAndUnsubscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), sm.getAuthorizationHeader(), 5000);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "RANDOM", app.getDefaultGraphURI("RANDOM"),
					app.getNamedGraphURI("RANDOM"), null, 5000);

		spuid = null;
		client.subscribe(request);

		while (subscribes.get() != 1) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to subscribe", subscribes.get() != 1);

		UnsubscribeRequest unsub;
		if (app.isSecure()) {
			unsub = new UnsubscribeRequest(spuid, sm.getAuthorizationHeader(), 5000);
		} else {
			unsub = new UnsubscribeRequest(spuid, null, 5000);
		}

		client.unsubscribe(unsub);

		while (subscribes.get() != 0) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to unsubscribe", subscribes.get() != 0);

		client.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: " + notify);
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("@onError: " + errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: " + spuid + " alias: " + alias);

		synchronized (subscribes) {
			this.spuid = spuid;
			subscribes.set(subscribes.get() + 1);
			subscribes.notify();
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe " + spuid);
		synchronized (subscribes) {
			subscribes.set(subscribes.get() - 1);
			subscribes.notify();
		}
	}
}