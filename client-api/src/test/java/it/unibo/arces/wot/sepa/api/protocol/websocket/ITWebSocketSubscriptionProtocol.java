package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class ITWebSocketSubscriptionProtocol implements ISubscriptionHandler {
    static {
        ConfigurationProvider.configureLogger();
    }

	protected final Logger logger = LogManager.getLogger();
	
	private static Sync sync = new Sync();
	private String spuid = null;
	
	private HashSet<Subscriber> subscribers = new HashSet<Subscriber>();
	
	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {

	}

	@Before
	public void before() {
		sync.reset();
		subscribers.clear();
	}

	@After
	public void after() throws IOException {
		for (Subscriber s : subscribers) s.close();
	}

	@Test (timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		Subscriber s = new Subscriber(1,this);
		subscribers.add(s);
		s.start();

		sync.waitSubscribes(1);	
	}

	@Test (timeout = 5000)
	public void MultipleSubscribes() throws IOException, SEPASecurityException, SEPAPropertiesException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			Subscriber s = new Subscriber(1,this);
			subscribers.add(s);
			s.start();
		}

		sync.waitSubscribes(n);
	}

	@Test (timeout = 5000)
	public void MultipleClientsAndMultipleSubscribes() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		int n = 5;
		int m = 5;

		for (int i = 0; i < m; i++) {
			Subscriber s = new Subscriber(n,this);
			subscribers.add(s);
			s.start();
		}

		sync.waitSubscribes(n*m);
	}

	@Test (timeout = 5000)
	public void SubscribeAndUnsubscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		spuid = null;
		
		Subscriber s = new Subscriber(1,this);
		subscribers.add(s);
		s.start();

		sync.waitSubscribes(1);	
		
		s.unsubscribe(spuid);

		sync.waitUnsubscribes(1);
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
		this.spuid = spuid;
		sync.subscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe " + spuid);
		
		sync.unsubscribe();
	}
}
