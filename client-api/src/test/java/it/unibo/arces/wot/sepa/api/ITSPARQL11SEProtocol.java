package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ITSPARQL11SEProtocol {
	protected final static Logger logger = LogManager.getLogger();

	protected static JSAP properties = null;
	private static SEPASecurityManager sm;
	private static SPARQL11Protocol client;

	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private final static Sync sync = new Sync();
	private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
	private final ArrayList<Publisher> publishers = new ArrayList<Publisher>();

	private static ConfigurationProvider provider;
	
	@BeforeClass
	public static void init() throws Exception {
		provider = new ConfigurationProvider();
		properties = provider.getJsap();

		if (properties.isSecure()) {
			sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",
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

		if (sm != null)
			client = new SPARQL11Protocol(sm);
		else
			client = new SPARQL11Protocol();

		subscribers.clear();
		publishers.clear();

		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));
			}

		}
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@After
	public void endTest() throws IOException, InterruptedException {
		if (client != null)
			client.close();

		for (Subscriber sub : subscribers)
			sub.close();
		
		for (Publisher pub : publishers) {
			pub.finish();
			pub.interrupt();
			pub.join();
		}
	}

	@Test(timeout = 5000)
	public void RegisterNotAllowed() throws SEPASecurityException, SEPAPropertiesException {
		if (sm != null) {
			Response response = sm.register(NOT_VALID_ID);
			assertFalse("Failed to register a not valid ID", !response.isError());
		}
	}

	@Test(timeout = 5000)
	public void Register() throws SEPASecurityException, SEPAPropertiesException {
		if (sm != null) {
			Response response = sm.register(VALID_ID);
			assertFalse("Failed to register a valid ID", response.isError());
		}
	}

	@Test(timeout = 5000)
	public void DeleteAll() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		// Delete all triples
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				ret = client.update(provider.buildUpdateRequest("DELETE_ALL", 5000,sm));
			}

		}
		assertFalse(String.valueOf(ret), ret.isError());

		// Evaluate if the store is empty
		ret = client.query(provider.buildQueryRequest("COUNT", 5000,sm));

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				ret = client.query(provider.buildQueryRequest("COUNT", 5000,sm));
			}

		}
		assertFalse(String.valueOf(ret), ret.isError());

		QueryResponse results = (QueryResponse) ret;
		assertFalse(String.valueOf(results), results.getBindingsResults().size() != 1);

		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse("Results are null " + String.valueOf(results), bindings.getValue("n") == null);
			assertFalse("RDF store is not empty " + String.valueOf(results), !bindings.getValue("n").equals("0"));
		}
	}

	@Test(timeout = 25000)
	public void RequestToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (sm != null) {
			for (int i = 0; i < 2000; i++) {
				String authorization = sm.getAuthorizationHeader();
				assertFalse("Failed to get authorization header", authorization == null);
				Thread.sleep(10);
			}
		}
	}

	@Test(timeout = 5000)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));
			}
		}
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("ALL", 5000,sm));
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				client.query(provider.buildQueryRequest("ALL", 5000,sm));
			}
		}
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void UpdateAndQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));

		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				ret = client.update(provider.buildUpdateRequest("VAIMEE", 5000,sm));
			}
		}
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.query(provider.buildQueryRequest("VAIMEE", 5000,sm));
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError()) {
				client.query(provider.buildQueryRequest("VAIMEE", 5000,sm));
			}
		}

		assertFalse(String.valueOf(ret), ret.isError());
		assertFalse(String.valueOf(ret), ((QueryResponse) ret).getBindingsResults().size() != 1);
	}

	@Test(timeout = 5000)
	public void Subscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
		subscribers.add(new Subscriber("ALL", sm, sync));

		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
				sync.getEvents() != subscribers.size());
	}

	@Test(timeout = 5000)
	public void Subscribe3xN()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
		int n = 10;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("ALL", sm, sync));
			subscribers.add(new Subscriber("RANDOM", sm, sync));
			subscribers.add(new Subscriber("RANDOM1", sm, sync));
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
		subscribers.add(new Subscriber("ALL", sm, sync));
		for (Subscriber sub : subscribers)
			sub.start();

		sync.waitSubscribes(subscribers.size());

		for (Subscriber sub : subscribers) sub.unsubscribe(sync.getSpuid());

		sync.waitEvents(subscribers.size());
		sync.waitUnsubscribes(subscribers.size());

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
				sync.getEvents() != subscribers.size());
		assertFalse("Unsubscribes:" + sync.getUnsubscribes() + "(" + subscribers.size() + ")",
				sync.getUnsubscribes() != subscribers.size());
	}

	@Test(timeout = 5000)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, InterruptedException {

		subscribers.add(new Subscriber("VAIMEE", sm, sync));
		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());

		publishers.add(new Publisher("VAIMEE", sm, 1));
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
			subscribers.add(new Subscriber("RANDOM", sm, sync));
			publishers.add(new Publisher("RANDOM", sm, n));
		}

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());

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
	public void UpdateHeavyLoad() throws InterruptedException, SEPAPropertiesException, SEPASecurityException {
		int n = 5;

		for (int i = 0; i < n; i++) {
			publishers.add(new Publisher("RANDOM", sm, n));
			publishers.add(new Publisher("RANDOM1", sm, n));
			publishers.add(new Publisher("VAIMEE", sm, n));
		}

		for (Publisher pub : publishers) pub.start();
		for (Publisher pub : publishers) pub.join();
	}

	/* To be used for long lasting test (30 min)*/
	//@Test(timeout = 1800000)
	public void StressTest() throws IOException, IllegalArgumentException, SEPAProtocolException,
			InterruptedException, SEPAPropertiesException, SEPASecurityException {
		int n = 50;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("ALL", sm, sync));
			subscribers.add(new Subscriber("RANDOM", sm, sync));
			subscribers.add(new Subscriber("RANDOM1", sm, sync));
		}


		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());
		sync.waitEvents(subscribers.size());
		
		assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")", sync.getEvents() != subscribers.size());
		
		int events = 4 * n * n * n;
		
		while (true) {
			publishers.clear();
			sync.reset();
			
			for (int i = 0; i < n; i++) {
				publishers.add(new Publisher("RANDOM", sm, n));
				publishers.add(new Publisher("RANDOM1", sm, n));
			}
			
			for (Publisher pub : publishers) pub.start();
			for (Publisher pub : publishers) pub.join();
			
			sync.waitEvents(events);

			assertFalse("Events:" + sync.getEvents() + "(" + events + ")", sync.getEvents() != events);
		}
	}

	@Test(timeout = 60000)
	public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {
		int n = 5;

		for (int i = 0; i < n; i++) {
			subscribers.add(new Subscriber("ALL", sm, sync));
			subscribers.add(new Subscriber("RANDOM", sm, sync));
			subscribers.add(new Subscriber("RANDOM1", sm, sync));

			publishers.add(new Publisher("RANDOM", sm, n));
			publishers.add(new Publisher("RANDOM1", sm, n));
		}

		int events = 4 * n * n * n + subscribers.size();

		for (Subscriber sub : subscribers) sub.start();

		sync.waitSubscribes(subscribers.size());

		for (Publisher pub : publishers) pub.start();

		sync.waitEvents(events);

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
				sync.getSubscribes() != subscribers.size());
		assertFalse("Events:" + sync.getEvents() + "(" + events + ")", sync.getEvents() != events);
	}
}