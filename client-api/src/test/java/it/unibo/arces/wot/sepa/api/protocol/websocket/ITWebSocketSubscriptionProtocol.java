package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.WebsocketSubscriptionProtocol;
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
	
	private static AtomicLong events = new AtomicLong(0);
	private static AtomicLong subscribes = new AtomicLong(0);
	private static AtomicLong brokens = new AtomicLong(0);
	
	private String spuid = null;
	private static final Object spuidMutex = new Object();
	
	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException {
		app = ConfigurationProvider.GetTestEnvConfiguration();
		if (app.isSecure())
			sm = new SEPASecurityManager(app.getAuthenticationProperties());
	}

	@Before
	public void before()
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, URISyntaxException {
		if (app.isSecure()) {
			sm.register("SEPATest");
			client = new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
					app.getSubscribePath(), sm, this);
		} else
			client = new WebsocketSubscriptionProtocol(app.getDefaultHost(), app.getSubscribePort(),
					app.getSubscribePath(), this);
		
		events.set(0);
		subscribes.set(0);
		brokens.set(0);
	}

	@Test(timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), sm.getAuthorizationHeader(), 5000);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), null, 5000);

		logger.debug(request);
		
		client.subscribe(request);
		
		while(subscribes.get() != 1) {
			synchronized(subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		client.close();
		
		assertFalse("Failed to subscribe",subscribes.get() != 1);
	}

	@Test(timeout = 20000)
	public void BrokenSockets() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		int n = 5;
		
		SubscribeRequest request;
		ArrayList<Thread> threadPoll = new ArrayList<Thread>();
		Thread th;
		
		for (int i = 0; i < n; i++) {
			if (app.isSecure()) {
				request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "all", app.getDefaultGraphURI("ALL"),
						app.getNamedGraphURI("ALL"), sm.getAuthorizationHeader(), 5000);
			} else
				request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "all", app.getDefaultGraphURI("ALL"),
						app.getNamedGraphURI("ALL"), null, 5000);
			th = new Thread(new WebsocketClient(app.getDefaultHost(), app.getSubscribePort(), app.getSubscribePath(), sm,
					request,this));
			threadPoll.add(th);
			th.start();

			if (app.isSecure()) {
				request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "random", app.getDefaultGraphURI("RANDOM"),
						app.getNamedGraphURI("RANDOM"), sm.getAuthorizationHeader(), 5000);
			} else
				request = new SubscribeRequest(app.getSPARQLQuery("RANDOM"), "random", app.getDefaultGraphURI("RANDOM"),
						app.getNamedGraphURI("RANDOM"), null, 5000);
			th = new Thread(new WebsocketClient(app.getDefaultHost(), app.getSubscribePort(), app.getSubscribePath(), sm,
					request,this));
			threadPoll.add(th);
			th.start();

			if (app.isSecure()) {
				request = new SubscribeRequest(app.getSPARQLQuery("RANDOM1"), "random1",
						app.getDefaultGraphURI("RANDOM1"), app.getNamedGraphURI("RANDOM1"), sm.getAuthorizationHeader(),
						5000);
			} else
				request = new SubscribeRequest(app.getSPARQLQuery("RANDOM1"), "random1",
						app.getDefaultGraphURI("RANDOM1"), app.getNamedGraphURI("RANDOM1"), null, 5000);
			th = new Thread(new WebsocketClient(app.getDefaultHost(), app.getSubscribePort(), app.getSubscribePath(), sm,
					request,this));
			threadPoll.add(th);
			th.start();
		}
		
		while(subscribes.get() != n * 3) {
			synchronized(subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to subscribe",subscribes.get() != n * 3);
		
		for (Thread th1 : threadPoll) {
			try {
				th1.join();
			} catch (InterruptedException e) {
				
			}
		}
		
		while(events.get() != n * 3) {
			synchronized(events) {
				try {
					events.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to receive all first notifications",events.get() != n * 3);
		
		while(brokens.get() != n * 3) {
			synchronized(brokens) {
				try {
					brokens.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to receive all broken notifications",brokens.get() != n * 3);
	}

	@Test(timeout = 5000)
	public void SubscribexN() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		int n = 100;
		
		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), sm.getAuthorizationHeader(), 500);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), null, 500);

		for (int i = 0; i < n; i++) {
			logger.debug(request);
			client.subscribe(request);
		}
		
		while(subscribes.get() < n) {
			synchronized(subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to subscribe",subscribes.get() != n);

		client.close();
	}

	@Test(timeout = 5000)
	public void SubscribeAndUnsubscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		SubscribeRequest request;
		if (app.isSecure()) {
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), sm.getAuthorizationHeader(), 5000);
		} else
			request = new SubscribeRequest(app.getSPARQLQuery("ALL"), "TEST_ALL_SUB", app.getDefaultGraphURI("ALL"),
					app.getNamedGraphURI("ALL"), null, 5000);

		spuid = null;
		client.subscribe(request);
		
		while(subscribes.get() != 1) {
			synchronized(subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to subscribe",subscribes.get() != 1);
		
		UnsubscribeRequest unsub;
		if (app.isSecure()) {
			unsub = new UnsubscribeRequest(spuid, sm.getAuthorizationHeader(), 5000);
		} else {
			unsub = new UnsubscribeRequest(spuid, null, 5000);
		}

		client.unsubscribe(unsub);
		
		while(subscribes.get() != 0) {
			synchronized(subscribes) {
				try {
					subscribes.wait();
				} catch (InterruptedException e) {
					
				}
			}
		}
		
		assertFalse("Failed to unsubscribe",subscribes.get() != 0);

		client.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: "+notify);
		
		synchronized(events) {
			events.set(events.get()+1);
			events.notify();
		}
		
		logger.debug("Number of events: "+events.get());
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
		
		synchronized(brokens) {
			brokens.set(brokens.get()+1);
			brokens.notify();
		}
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("@onError: "+errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: "+spuid+" alias: "+alias);
		
		synchronized(subscribes) {
			subscribes.set(subscribes.get()+1);
			subscribes.notify();
		}
		
		synchronized (spuidMutex) {
			this.spuid = spuid;
			spuidMutex.notify();
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe "+spuid);
		synchronized(subscribes) {
			subscribes.set(subscribes.get()-1);
			subscribes.notify();
		}
	}
}
