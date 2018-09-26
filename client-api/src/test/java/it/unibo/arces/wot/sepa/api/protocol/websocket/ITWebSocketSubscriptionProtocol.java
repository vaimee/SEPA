package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

import static org.junit.Assert.*;

public class ITWebSocketSubscriptionProtocol implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private WebsocketSubscriptionProtocol client = null;
	
	private static SEPASecurityManager sm = null;
	
	private static AtomicLong subscribes = new AtomicLong(0);
	private String spuid = null;

	private static ConfigurationProvider provider;
	
	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		provider = new ConfigurationProvider();

		if (provider.getJsap().isSecure()) {
			ClassLoader classLoader = ITSPARQL11SEProtocol.class.getClassLoader();
			File keyFile = new File(classLoader.getResource("sepa.jks").getFile());
			sm = new SEPASecurityManager(keyFile.getPath(), "sepa2017", "sepa2017",
					provider.getJsap().getAuthenticationProperties());
			sm.register("SEPATest");
		}
	}

	@Before
	public void before()
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, URISyntaxException {
		if (provider.getJsap().isSecure()) {
			client = new WebsocketSubscriptionProtocol(provider.getJsap().getDefaultHost(), provider.getJsap().getSubscribePort(),
					provider.getJsap().getSubscribePath());
			client.enableSecurity(sm);
		} else
			client = new WebsocketSubscriptionProtocol(provider.getJsap().getDefaultHost(), provider.getJsap().getSubscribePort(),
					provider.getJsap().getSubscribePath());

		client.setHandler(this);
		
		subscribes.set(0);
	}

	@After
	public void after() {

	}

	@Test (timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		client.subscribe(provider.buildSubscribeRequest("RANDOM", 5000, sm));

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

	@Test (timeout = 5000)
	public void MultipleSubscribes() throws IOException {
		int n = 95;

		for (int i = 0; i < n; i++) {
			try {
				client.subscribe(provider.buildSubscribeRequest("RANDOM", 5000, sm));
			} catch (SEPAProtocolException e) {
				logger.error(e.getMessage());
			}
		}

		while (subscribes.get() < n) {
			synchronized (subscribes) {
				try {
					subscribes.wait(5000);
					logger.warn("Subscribes: "+subscribes.get());
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to subscribe", subscribes.get() != n);

		client.close();
	}

	@Test (timeout = 5000)
	public void MultipleClientsAndMultipleSubscribes() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		int n = 10;
		int m = 5;

		for (int i = 0; i < m; i++) {
			new Subscriber(n,this).start();
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
		private final WebsocketSubscriptionProtocol client;
		private final int n;

		public Subscriber(int n,ISubscriptionHandler handler) throws SEPASecurityException {
			client = new WebsocketSubscriptionProtocol(provider.getJsap().getDefaultHost(), provider.getJsap().getSubscribePort(),
					provider.getJsap().getSubscribePath());
			
			if (provider.getJsap().isSecure()) client.enableSecurity(sm);
			client.setHandler(handler);
			
			this.n = n;
		}

		public void run() {
			for (int j = 0; j < n; j++) {
				try {
					client.subscribe(provider.buildSubscribeRequest("RANDOM", 500, sm));
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}

	@Test (timeout = 5000)
	public void SubscribeAndUnsubscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		spuid = null;
		
		client.subscribe(provider.buildSubscribeRequest("RANDOM", 500, sm));

		while (subscribes.get() != 1) {
			synchronized (subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {

				}
			}
		}

		assertFalse("Failed to subscribe", subscribes.get() != 1);

		client.unsubscribe(provider.buildUnsubscribeRequest(spuid, 500, sm));

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
