package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocols.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class ITSPARQL11SEProtocol implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	protected static JSAP properties = null;
	private static SEPASecurityManager sm;
	private static SPARQL11SEProtocol client = null;

	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private static AtomicLong events = new AtomicLong(0);
	private static AtomicLong subscribes = new AtomicLong(0);
	private String spuid = null;
	private static final Object spuidMutex = new Object();

	@BeforeClass
	public static void init() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();

		if (properties.isSecure()) {
			sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",
					new AuthenticationProperties(properties.getFileName()));

			// Registration
			Response response = sm.register(VALID_ID);
			assertFalse("Failed to register a valid ID", response.isError());
		}
	}

	@AfterClass
	public static void dispose() throws IOException {

	}

	@Before
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException {

		SubscriptionProtocol protocol;

		if (sm == null) {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), this);
			client = new SPARQL11SEProtocol(protocol);
		} else {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), sm, this);
			client = new SPARQL11SEProtocol(protocol, sm);
		}

		assertFalse("Failed to create SPARQL11SEProtocol", client == null);

		Response ret = client.update(buildUpdateRequest("DELETE_ALL", 5000));

		assertFalse("Failed to create Delete all triples", ret.isError());

		subscribes.set(0);
		events.set(0);
	}

	@After
	public void endTest() throws IOException {
		if (client != null)
			client.close();
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
	public void DeleteAll() throws SEPAPropertiesException, SEPASecurityException {
		// Delete all triples
		Response ret = client.update(buildUpdateRequest("DELETE_ALL", 5000));
		assertFalse(String.valueOf(ret), ret.isError());

		// Check that the store size is 0
		ret = client.query(buildQueryRequest("COUNT", 5000));
		assertFalse(String.valueOf(ret), ret.isError());

		QueryResponse results = (QueryResponse) ret;
		assertFalse(String.valueOf(results), results.getBindingsResults().size() != 1);

		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse(String.valueOf(results), bindings.getValue("n") == null);
			assertFalse(String.valueOf(results), !bindings.getValue("n").equals("0"));
		}
	}

	@Test(timeout = 15000)
	public void GetAuthorizationHeader() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (sm != null) {
			sm.register(VALID_ID);
			sm.getAuthorizationHeader();

			// For testing the token expires in 5 seconds
			for (int i = 0; i < 10; i++) {
				sm.getAuthorizationHeader();
				Thread.sleep(1000);
			}
		}
	}

	@Test(timeout = 1000)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.update(buildUpdateRequest("VAIMEE", 5000));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 1000)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.query(buildQueryRequest("ALL", 5000));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 1000)
	public void UpdateAndQuery() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.update(buildUpdateRequest("VAIMEE", 5000));
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.query(buildQueryRequest("VAIMEE", 5000));

		assertFalse(String.valueOf(ret), ret.isError());
		assertFalse(String.valueOf(ret), ((QueryResponse) ret).getBindingsResults().size() != 1);
	}

	@Test(timeout = 1000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		client.subscribe(buildSubscribeRequest("ALL", 5000));

		synchronized (subscribes) {
			try {
				subscribes.wait();
			} catch (InterruptedException e) {

			}
		}

		client.close();

		assertFalse("Failed to subscribe", subscribes.get() == 0);
	}

	@Test(timeout = 1000)
	public void Subscribe3xN() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		int n = 33;
		int total = 3 * n;

		for (int i = 0; i < n; i++) {
			client.subscribe(buildSubscribeRequest("ALL", 5000));
			client.subscribe(buildSubscribeRequest("RANDOM", 5000));
			client.subscribe(buildSubscribeRequest("RANDOM1", 5000));
		}

		while (subscribes.get() < total) {
			synchronized (subscribes) {
				try {
					subscribes.wait(2000);
				} catch (InterruptedException e) {

				}
			}
		}

		while (events.get() < total) {
			synchronized (events) {
				try {
					events.wait(2000);
				} catch (InterruptedException e) {

				}
			}
		}

		client.close();

		assertFalse("Failed to subscribe (" + subscribes.get() + " on " + total + ")", subscribes.get() != total);
		assertFalse("Failed to receive first results. Received: " + events.get() + " on: " + total + " events",
				events.get() != total);
	}

	@Test(timeout = 1000)
	public void Unsubscribe() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
		spuid = null;

		client.subscribe(buildSubscribeRequest("ALL", 5000));

		while (spuid == null) {
			synchronized (spuidMutex) {
				try {
					spuidMutex.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}

		while (events.get() < 1) {
			synchronized (events) {
				try {
					events.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}

		if (spuid != null) {
			client.unsubscribe(buildUnsubscribeRequest(spuid, 5000));

			while (subscribes.get() != 0) {
				synchronized (subscribes) {
					try {
						subscribes.wait(1000);
					} catch (InterruptedException e) {

					}
				}
			}
		}

		client.close();

		assertFalse("Failed to subscribe", spuid == null);
		assertFalse("Received: " + events.get(), events.get() != 1);
		assertFalse("Failed to unsubscribe " + subscribes.get(), subscribes.get() != 0);
	}

	@Test(timeout = 1000)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, InterruptedException {

		Subscriber sub = new Subscriber("VAIMEE", properties, sm, this);
		sub.start();

		while (spuid == null) {
			synchronized (spuidMutex) {
				try {
					spuidMutex.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}

		synchronized (events) {
			while (events.get() < 1)
				try {
					events.wait(1000);
				} catch (InterruptedException e) {

				}
		}

		Publisher pub = null;
		
		if (spuid != null) {
			pub = new Publisher("VAIMEE", properties, sm, 1);
			pub.start();

			synchronized (events) {
				while (events.get() < 2)
					try {
						events.wait(1000);
					} catch (InterruptedException e) {

					}
			}
		}
		
		sub.finish();
		sub.interrupt();
		
		if (pub != null) {
			pub.interrupt();
			pub.join(1000);
		}
		
		sub.join(1000);
		
		assertFalse("Failed to subscribe", spuid == null);
		assertFalse("Notification not received " + events.get(), events.get() != 2);
	}

	@Test(timeout = 5000)
	public void NotifyNxN() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {

		int n = 10;
		int total = n * n * n;

		ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
		ArrayList<Publisher> publishers = new ArrayList<Publisher>();
		
		Publisher pub;
		Subscriber sub;

		for (int i = 0; i < n; i++) {
			sub = new Subscriber("RANDOM", properties, sm, this);
			sub.start();
			subscribers.add(sub);
		}

		while (subscribes.get() < n) {
			synchronized (subscribes) {
				try {
					subscribes.wait(5000);
				} catch (InterruptedException e) {

				}
			}
		}

		synchronized (events) {
			while (events.get() < n)
				try {
					events.wait(5000);
				} catch (InterruptedException e) {

				}
		}

		for (int i = 0; i < n; i++) {
			pub = new Publisher("RANDOM", properties, sm, n);
			publishers.add(pub);
			pub.start();
		}

		for (Publisher pb1 : publishers)
			pb1.join();

		synchronized (events) {
			while (events.get() < total+n)
				try {
					events.wait(5000);
				} catch (InterruptedException e) {

				}
		}
		
		for (Subscriber sb1 : subscribers) {
			sb1.finish();
			sb1.interrupt();
			sb1.join(100);
		}
		
		assertFalse("Failed to subscribe (" + subscribes.get() + " on " + n + ")", subscribes.get() != n);	
		assertFalse("Received: " + events.get() + " on: " + total+n + " events", events.get() != total+n);
	}

	@Test(timeout = 10000)
	public void UpdateHeavyLoad() throws InterruptedException {
		int n = 10;

		ArrayList<Publisher> pool = new ArrayList<Publisher>();
		Publisher pb;

		for (int i = 0; i < n; i++) {
			pb = new Publisher("RANDOM", properties, sm, n);
			pool.add(pb);
			pb.start();
			pb = new Publisher("RANDOM1", properties, sm, n);
			pool.add(pb);
			pb.start();
			pb = new Publisher("VAIMEE", properties, sm, n);
			pool.add(pb);
			pb.start();
		}

		for (Publisher pb1 : pool)
			pb1.join(n*500);
	}

	@Test(timeout = 10000)
	public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {
		int n = 10;

		ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
		ArrayList<Publisher> publishers = new ArrayList<Publisher>();
		
		Publisher pub;
		Subscriber sub;

		int total = 4 * n * n * n;

		for (int i = 0; i < n; i++) {
			sub = new Subscriber("ALL", properties, sm, this);
			sub.start();
			subscribers.add(sub);
			
			sub =new Subscriber("RANDOM", properties, sm, this);
			sub.start();
			subscribers.add(sub);
			
			sub = new Subscriber("RANDOM1", properties, sm, this);
			sub.start();
			subscribers.add(sub);
		}

		while (subscribes.get() < 3 * n) {
			synchronized (subscribes) {
				try {
					subscribes.wait(10000);
				} catch (InterruptedException e) {

				}
			}
		}

		synchronized (events) {
			while (events.get() < 3 * n)
				try {
					events.wait(5000);
				} catch (InterruptedException e) {

				}
		}

		for (int i = 0; i < n; i++) {
			pub = new Publisher("RANDOM", properties, sm, n);
			publishers.add(pub);
			pub.start();
			
			pub = new Publisher("RANDOM1", properties, sm, n);
			publishers.add(pub);
			pub.start();
		}

		for (Publisher pb1 : publishers)
			pb1.join(n*500);

		synchronized (events) {
			while (events.get() < total+ 3*n)
				try {
					events.wait(5000);
				} catch (InterruptedException e) {

				}
		}
		
		for (Subscriber sb1 : subscribers) {
			sb1.finish();
			sb1.interrupt();
			sb1.join(100);
		}

		assertFalse("Failed to subscribe (" + subscribes.get() + " on " + 3 * n + ")", subscribes.get() != 3 * n);
		assertFalse("Events not received (" + events.get() + " on " + total + 3*n + ")", events.get() != total+3*n);
	}

	protected static UpdateRequest buildUpdateRequest(String id, long timeout)
			throws SEPAPropertiesException, SEPASecurityException {
		HTTPMethod method = properties.getUpdateMethod(id);
		String scheme = properties.getUpdateProtocolScheme(id);
		String host = properties.getUpdateHost(id);
		int port = properties.getUpdatePort(id);
		String path = properties.getUpdatePath(id);
		String sparql = properties.getSPARQLUpdate(id);
		String graphUri = properties.getUsingGraphURI(id);
		String namedGraphUri = properties.getUsingNamedGraphURI(id);

		String authorization = null;
		if (sm != null)
			authorization = sm.getAuthorizationHeader();

		return new UpdateRequest(method, scheme, host, port, path, sparql, graphUri, namedGraphUri, authorization,
				timeout);
	}

	protected static QueryRequest buildQueryRequest(String id, long timeout)
			throws SEPAPropertiesException, SEPASecurityException {
		HTTPMethod method = properties.getQueryMethod(id);
		String scheme = properties.getQueryProtocolScheme(id);
		String host = properties.getQueryHost(id);
		int port = properties.getQueryPort(id);
		String path = properties.getQueryPath(id);
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		String authorization = null;
		if (sm != null)
			authorization = sm.getAuthorizationHeader();

		return new QueryRequest(method, scheme, host, port, path, sparql, graphUri, namedGraphUri, authorization,
				timeout);
	}

	protected static SubscribeRequest buildSubscribeRequest(String id, long timeout)
			throws SEPAPropertiesException, SEPASecurityException {
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		String authorization = null;
		if (sm != null)
			authorization = sm.getAuthorizationHeader();

		return new SubscribeRequest(sparql, null, graphUri, namedGraphUri, authorization, timeout);
	}

	private UnsubscribeRequest buildUnsubscribeRequest(String spuid, long timeout)
			throws SEPAPropertiesException, SEPASecurityException {
		String authorization = null;
		if (sm != null)
			authorization = sm.getAuthorizationHeader();

		return new UnsubscribeRequest(spuid, authorization, timeout);
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.trace("@onSemanticEvent " + notify);
		synchronized (events) {
			events.set(events.get() + 1);
			logger.debug("Notifications received: " + events.get());
			events.notify();
		}
	}

	@Override
	public void onBrokenConnection() {
		logger.trace("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.trace("@onError");
		logger.error(errorResponse.toString());
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.trace("@onSubscribe: " + spuid + " alias: " + alias);

		synchronized (subscribes) {
			subscribes.set(subscribes.get() + 1);
			subscribes.notify();
		}

		synchronized (spuidMutex) {
			this.spuid = spuid;
			spuidMutex.notify();
		}

	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.trace("@onUnsubscribe: " + spuid);

		synchronized (subscribes) {
			subscribes.set(subscribes.get() - 1);
			subscribes.notify();
		}
	}
}