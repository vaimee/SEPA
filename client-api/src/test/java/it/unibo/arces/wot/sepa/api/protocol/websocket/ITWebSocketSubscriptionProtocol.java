package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class ITWebSocketSubscriptionProtocol {
	protected final Logger logger = LogManager.getLogger();

	private static Sync sync = new Sync();

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
	public void after() throws IOException, SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		for (Subscriber s : subscribers)
			s.close();
		sync.close();
	}

	@Test(timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		Subscriber s = new Subscriber(1, sync);
		subscribers.add(s);
		s.start();

		sync.waitSubscribes(1);
	}

	@Test(timeout = 5000)
	public void MultipleSubscribes()
			throws IOException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			Subscriber s = new Subscriber(1, sync);
			subscribers.add(s);
			s.start();
		}

		sync.waitSubscribes(n);
	}

	@Test (timeout = 10000)
	public void MultipleClientsAndMultipleSubscribes()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		int n = 5;
		int m = 5;

		for (int i = 0; i < m; i++) {
			Subscriber s = new Subscriber(n, sync);
			subscribers.add(s);
			s.start();
		}

		sync.waitSubscribes(n * m);
	}

	@Test(timeout = 5000)
	public void SubscribeAndUnsubscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		Subscriber s = new Subscriber(1, sync);
		subscribers.add(s);
		s.start();

		sync.waitSubscribes(1);

		s.unsubscribe();

		sync.waitUnsubscribes(1);
	}
}
