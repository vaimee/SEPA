package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ITSPARQL11SEProtocol {
	private static JSAP properties = null;
	private static ConfigurationProvider provider;
	
	private static SEPASecurityManager sm;
	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private final static Sync sync = new Sync();
	
	private static SPARQL11Protocol  client;
	private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
	private final ArrayList<Publisher> publishers = new ArrayList<Publisher>();
	
	@BeforeClass
	public static void init() throws Exception {
		provider = new ConfigurationProvider();
		properties = provider.getJsap();

		if (properties.isSecure()) {
			ClassLoader classLoader = ITSPARQL11SEProtocol.class.getClassLoader();
			File keyFile = new File(classLoader.getResource("sepa.jks").getFile());
			sm = new SEPASecurityManager(keyFile.getPath(), "sepa2017", "sepa2017",
					new AuthenticationProperties(properties.getFileName()));

			// Registration
			Response response = sm.register(VALID_ID);
			assertFalse("Failed to register a valid ID", response.isError());
		}
	}

	@Before
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {

		sync.reset();

		if (properties.isSecure())
			client = new SPARQL11Protocol(sm);
		else
			client = new SPARQL11Protocol();

		subscribers.clear();
		publishers.clear();

		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));
		while(ret.isError() && ((ErrorResponse)ret).getStatusCode() == 400 &&
				properties.isSecure()){
			sm.forceRefreshToken();
			ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));
		}
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@After
	public void endTest() throws IOException, InterruptedException {
		client.close();

		for (Subscriber sub : subscribers) sub.close();
		
		for (Publisher pub : publishers) pub.close();
	}

	@Test(timeout = 5000)
	public void RegisterNotAllowed() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = sm.register(NOT_VALID_ID);
			assertFalse("Failed to register a not valid ID", !response.isError());
		}
	}

	@Test(timeout = 5000)
	public void Register() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = sm.register(VALID_ID);
			assertFalse("Failed to register a valid ID", response.isError());
		}
	}

	@Test(timeout = 5000)
	public void DeleteAllWithCheck() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		// Delete all triples
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());

		// Evaluate if the store is empty
		ret = client.query(provider.buildQueryRequest("COUNT", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());

		QueryResponse results = (QueryResponse) ret;
		assertFalse(String.valueOf(results), results.getBindingsResults().size() != 1);

		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse("Results are null " + String.valueOf(results), bindings.getValue("n") == null);
			assertFalse("RDF store is not empty " + String.valueOf(results), !bindings.getValue("n").equals("0"));
		}
	}

	@Test(timeout = 15000)
	public void RequestToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (properties.isSecure()) {
			for (int i = 0; i < 100; i++) {
				String authorization = sm.getAuthorizationHeader();
				assertFalse("Failed to get authorization header", authorization == null);
				Thread.sleep(100);
			}
		}
	}

	@Test(timeout = 15000)
	public void UseExpiredToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (properties.isSecure()) {
			String authorization = sm.getAuthorizationHeader();

			assertFalse("Failed to get authorization header", authorization == null);

			final long expiringTime = 5000;
			Thread.sleep(expiringTime+1000);
			final Response tokenTest = client.query(provider.buildQueryRequest("ALL", 5000, authorization));

			assertTrue("Response should be error since the token is expired",tokenTest.isError());
		}
	}

	@Test(timeout = 1000)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());
	}
	
	@Test(timeout = 1000)
	public void MalformedUpdate() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("WRONG", 5000,sm));
		assertTrue(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("ALL", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());
	}
	
	@Test(timeout = 5000)
	public void MalformedQuery() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("WRONG", 5000,sm));
		assertTrue(String.valueOf(ret), ret.isError());
	}

	@Test (timeout = 5000)
	public void UpdateAndQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.query(provider.buildQueryRequest("VAIMEE", 5000,sm));
		assertFalse(String.valueOf(ret), ret.isError());
		
		assertFalse(String.valueOf(ret), ((QueryResponse) ret).getBindingsResults().size() != 1);
	}

	@Test(timeout = 5000)
	public void Subscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
		subscribers.add(new Subscriber("ALL", sync));

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
				sync.getEvents() != subscribers.size());
	}

	@Test (timeout = 30000)
	public void Subscribe3xN()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
		int n = 5;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("ALL", sync));
			subscribers.add(new Subscriber("RANDOM", sync));
			subscribers.add(new Subscriber("RANDOM1", sync));
		}

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
				sync.getEvents() != subscribers.size());
	}

	@Test(timeout = 5000)
	public void Unsubscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
		
		subscribers.add(new Subscriber("ALL", sync));
		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());
		
		for (Subscriber sub : subscribers) sub.unsubscribe(sync.getSpuid());
		sync.waitUnsubscribes(subscribers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
				sync.getEvents() != subscribers.size());
		assertFalse("Unsubscribes:" + sync.getUnsubscribes() + "(" + subscribers.size() + ")",
				sync.getUnsubscribes() != subscribers.size());
	}

	@Test(timeout = 1000)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, InterruptedException {

		subscribers.add(new Subscriber("VAIMEE", sync));
		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());

		publishers.add(new Publisher("VAIMEE", 1));
		for (Publisher pub : publishers) pub.start();

		sync.waitEvents(1);

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(1)", sync.getEvents() != 1);
	}

	@Test(timeout = 60000)
	public void NotifyNxN() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {

		int n = 5;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("RANDOM", sync));
			publishers.add(new Publisher("RANDOM", n));
		}

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());
		
		for (Publisher pub : publishers) pub.start();

		sync.waitEvents(subscribers.size() + subscribers.size() * publishers.size() * publishers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse(
				"Events:" + sync.getEvents() + "(" + subscribers.size()
						+ subscribers.size() * publishers.size() * publishers.size() + ")",
				sync.getEvents() != subscribers.size() + subscribers.size() * publishers.size() * publishers.size());
	}
	
	@Test(timeout = 60000)
	public void NotifyNx2NWithMalformedUpdates() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {

		int n = 5;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("RANDOM", sync));
			publishers.add(new Publisher("RANDOM", n));
			publishers.add(new Publisher("WRONG", n));
		}

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());
		
		for (Publisher pub : publishers) pub.start();

		sync.waitEvents(subscribers.size() + subscribers.size() * (publishers.size()/2) * (publishers.size()/2));

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse(
				"Events:" + sync.getEvents() + "(" + subscribers.size()
						+ subscribers.size() * (publishers.size()/2) * (publishers.size()/2) + ")",
				sync.getEvents() != subscribers.size() + subscribers.size() * (publishers.size()/2) * (publishers.size()/2));
	}

	@Test(timeout = 60000)
	public void UpdateHeavyLoad() throws InterruptedException, SEPAPropertiesException, SEPASecurityException {
		int n = 5;

		for (int i = 0; i < n; i++) {
			publishers.add(new Publisher("RANDOM", n));
			publishers.add(new Publisher("RANDOM1", n));
			publishers.add(new Publisher("VAIMEE", n));
		}

		for (Publisher pub : publishers) pub.start();
		
		// Wait all publishers to complete
		for (Publisher pub : publishers) pub.join();
	}

	@Test(timeout = 60000)
	public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {
		int n = 5;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("ALL", sync));
			subscribers.add(new Subscriber("RANDOM", sync));
			subscribers.add(new Subscriber("RANDOM1", sync));

			publishers.add(new Publisher("RANDOM", n));
			publishers.add(new Publisher("RANDOM1", n));
		}

		int events = 4 * n * n * n + subscribers.size();

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		for (Publisher pub : publishers) pub.start();

		sync.waitEvents(events);
		
		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + events + ")", sync.getEvents() != events);
	}
}