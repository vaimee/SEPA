package com.vaimee.sepa.stress;

import com.vaimee.sepa.ConfigurationProvider;
import com.vaimee.sepa.Publisher;
import com.vaimee.sepa.Subscriber;
import com.vaimee.sepa.Sync;
import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.logging.Logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class StressUsingSPARQLProtocol {
	private static ConfigurationProvider provider;

	private static Sync sync;

	private static SPARQL11Protocol client;

	private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
	private final ArrayList<Publisher> publishers = new ArrayList<Publisher>();

	@BeforeEach
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {
		provider = new ConfigurationProvider();
		sync = new Sync();
		client = new SPARQL11Protocol(provider.getClientSecurityManager());

		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));

		Logging.debug(ret);

		assertFalse(ret.isError(), String.valueOf(ret));

		subscribers.clear();
		publishers.clear();

		sync.reset();
	}

	@AfterEach
	public void endTest() throws IOException, InterruptedException {
		for (Subscriber sub : subscribers)
			sub.close();

		for (Publisher pub : publishers)
			pub.close();
		
		client.close();
		
		provider.close();
		
		Thread.sleep(ConfigurationProvider.SLEEP);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(60)
	public void Subscribe3xN()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException, IOException, URISyntaxException {
		int n = 10;
		
		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber(provider, "ALL", sync));
			subscribers.add(new Subscriber(provider, "RANDOM", sync));
			subscribers.add(new Subscriber(provider, "RANDOM1", sync));
		}

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		assertFalse(sync.getSubscribes() != subscribers.size(),
				"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
		assertFalse(sync.getEvents() != subscribers.size(),
				"Events:" + sync.getEvents() + "(" + subscribers.size() + ")");
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(60)
	public void NotifyNxN() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException, URISyntaxException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber(provider, "RANDOM", sync));
			publishers.add(new Publisher(provider, "RANDOM", n));
		}

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		for (Publisher pub : publishers)
			pub.start();

		sync.waitEvents(subscribers.size() + subscribers.size() * publishers.size() * publishers.size());

		assertFalse(sync.getSubscribes() != subscribers.size(),
				"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
		assertFalse(sync.getEvents() != subscribers.size() + subscribers.size() * publishers.size() * publishers.size(),
				"Events:" + sync.getEvents() + "(" + subscribers.size()
						+ subscribers.size() * publishers.size() * publishers.size() + ")");
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void NotifyNx2NWithMalformedUpdates() throws IOException, IllegalArgumentException, SEPAProtocolException,
			InterruptedException, SEPAPropertiesException, SEPASecurityException, URISyntaxException {
		int n = 4;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber(provider, "RANDOM", sync));
			publishers.add(new Publisher(provider, "RANDOM", n));
			publishers.add(new Publisher(provider, "WRONG", n));
		}

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		for (Publisher pub : publishers)
			pub.start();

		sync.waitEvents(subscribers.size() + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2));

		assertFalse(sync.getSubscribes() != subscribers.size(),
				"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
		assertFalse(
				sync.getEvents() != subscribers.size()
						+ subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2),
				"Events:" + sync.getEvents() + "(" + subscribers.size()
						+ subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2) + ")");
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(60)
	public void UpdateHeavyLoad() throws InterruptedException, SEPAPropertiesException, SEPASecurityException, IOException, SEPAProtocolException, URISyntaxException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			publishers.add(new Publisher(provider, "RANDOM", n));
			publishers.add(new Publisher(provider, "RANDOM1", n));
			publishers.add(new Publisher(provider, "VAIMEE", n));
		}

		for (Publisher pub : publishers)
			pub.start();

		// Wait all publishers to complete
		for (Publisher pub : publishers)
			pub.join();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(60)
	public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException, URISyntaxException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber(provider, "ALL", sync));
			subscribers.add(new Subscriber(provider, "RANDOM", sync));
			subscribers.add(new Subscriber(provider, "RANDOM1", sync));

			publishers.add(new Publisher(provider, "RANDOM", n));
			publishers.add(new Publisher(provider, "RANDOM1", n));
		}

		int events = 4 * n * n * n + subscribers.size();

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		for (Publisher pub : publishers)
			pub.start();

		sync.waitEvents(events);

		assertFalse(sync.getSubscribes() != subscribers.size(),
				"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
		assertFalse(sync.getEvents() != events, "Events:" + sync.getEvents() + "(" + events + ")");
	}
}
