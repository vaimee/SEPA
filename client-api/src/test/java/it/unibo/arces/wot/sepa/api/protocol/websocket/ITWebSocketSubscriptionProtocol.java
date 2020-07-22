package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

import it.unibo.arces.wot.sepa.ConfigurationProvider;

public class ITWebSocketSubscriptionProtocol {
	protected static final Logger logger = LogManager.getLogger();

	private static Sync sync = new Sync();

	private HashSet<Subscriber> subscribers = new HashSet<Subscriber>();

	@BeforeAll
	public static void init() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {

	}
	
	@AfterAll
	public static void end() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		logger.debug("end");
	}

	@BeforeEach
	public void before() {
		sync.reset();
		subscribers.clear();
	}

	@AfterEach
	public void after() throws IOException, SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		for (Subscriber s : subscribers)
			s.close();
		sync.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//(timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		Subscriber s = new Subscriber(1, sync);
		subscribers.add(s);
		s.start();

		sync.waitSubscribes(1);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//(timeout = 5000)
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

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//(timeout = 5000)
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

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//(timeout = 5000)
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
