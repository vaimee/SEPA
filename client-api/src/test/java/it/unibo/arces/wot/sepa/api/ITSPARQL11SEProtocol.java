package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class ITSPARQL11SEProtocol {
	protected static JSAP properties = null;
	private static SEPASecurityManager sm;
	private static SPARQL11SEProtocol client = null;

	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private static AtomicLong mutex = new AtomicLong(0);
	
	@BeforeClass
	public static void init() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();

		if (properties.isSecure()) {
			sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",
					new AuthenticationProperties(properties.getFileName()));

			// Registration
			Response response = sm.register(NOT_VALID_ID);
			assertFalse("Failed to register a not valid ID", !response.isError());
			response = sm.register(VALID_ID);
			assertFalse("Failed to register a valid ID", response.isError());
		}
	}

	@AfterClass
	public static void dispose() throws IOException {

	}

	@Before
	public void beginTest() throws IOException, IllegalArgumentException, SEPAProtocolException,
			SEPAPropertiesException, SEPASecurityException {

		ISubscriptionProtocol protocol = new WebSocketSubscriptionProtocol(properties.getDefaultHost(),properties.getSubscribePort(),properties.getSubscribePath());
		
		if (sm == null)
			client = new SPARQL11SEProtocol(protocol, new MockSubscriptionHandler());
		else
			client = new SPARQL11SEProtocol(protocol, new MockSubscriptionHandler(), sm);

		assertFalse("Failed to create SPARQL11SEProtocol", client == null);

		// Delete all triples
		Response ret = client.update(buildUpdateRequest("DELETE_ALL",5000));
		assertFalse(String.valueOf(ret), ret.isError());

		// Check that the store size is 0
		ret = client.query(buildQueryRequest("COUNT",5000));
		assertFalse(String.valueOf(ret), ret.isError());
		QueryResponse results = (QueryResponse) ret;
		assertFalse(String.valueOf(results), results.getBindingsResults().size() != 1);
		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse(String.valueOf(results), bindings.getValue("n") == null);
			assertTrue(String.valueOf(results), bindings.getValue("n").equals("0"));
		}
	}

	@After
	public void endTest() throws IOException {
		if (client != null)
			client.close();
	}

	@Test(timeout = 5000)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.update(buildUpdateRequest("VAIMEE",5000));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.query(buildQueryRequest("ALL",5000));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void UpdateAndQuery() throws IOException, SEPAPropertiesException, SEPASecurityException {
		Response ret = client.update(buildUpdateRequest("VAIMEE",5000));
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.query(buildQueryRequest("ALL",5000));
		assertFalse(String.valueOf(ret), ret.isError());
		assertFalse(String.valueOf(ret), ((QueryResponse) ret).getBindingsResults().size() != 1);
	}

	@Test(timeout = 5000)
	public void Subscribe() throws SEPAPropertiesException, SEPASecurityException {
		Response ret = client.subscribe(buildSubscribeRequest("ALL"));
		assertFalse(String.valueOf(ret), ret.isError());
	}
	
	@Test(timeout = 5000)
	public void Subscribex10() throws SEPAPropertiesException, SEPASecurityException {
		for (int i= 0; i < 10 ; i++) {
			Response ret = client.subscribe(buildSubscribeRequest("ALL"));
			assertFalse("Failed at: "+i+" "+String.valueOf(ret), ret.isError());
		}
	}

	@Test(timeout = 5000)
	public void Unsubscribe() throws SEPAPropertiesException, SEPASecurityException {
		Response ret = client.subscribe(buildSubscribeRequest("ALL"));
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.unsubscribe(buildUnsubscribeRequest(((SubscribeResponse)ret).getSpuid()));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {

		new Subscriber("ALL",properties,sm,mutex).start();

		Thread.sleep(1000);

		Update();

		synchronized (mutex) {
			mutex.wait();
		}
	}
	
	@Test(timeout = 5000)
	public void NotifyNxM() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {
		int n = 10;
		
		mutex.set(n*n*n);
		
		for (int i=0; i < n; i++) new Subscriber("ALL",properties,sm,mutex).start();

		Thread.sleep(1000);

		for (int i=0; i < n; i++) new Publisher("RANDOM",properties,sm,n).start();

		synchronized (mutex) {
			while(mutex.get() > 0) mutex.wait();
		}
	}

	@Test(timeout = 60000)
	public void Notify2Nx3M() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
			SEPAPropertiesException, SEPASecurityException {
		int n = 10;	
		int updates = n*n*n;
		
		int nAll = updates * n;
		int nRandom = n * n * n;
		int nRandom1 = n * n * n;
		
		mutex.set(nAll+nRandom+nRandom1);
		
		for (int i=0; i < n; i++) {
			new Subscriber("ALL",properties,sm,mutex).start();
			new Subscriber("RANDOM",properties,sm,mutex).start();
			new Subscriber("RANDOM1",properties,sm,mutex).start();
		}

		Thread.sleep(1000);

		for (int i=0; i < n; i++) {
			new Publisher("RANDOM",properties,sm,n).start();
			new Publisher("RANDOM1",properties,sm,n).start();
		}

		synchronized (mutex) {
			while(mutex.get() > 0) mutex.wait();
		}
	}
	
	protected static UpdateRequest buildUpdateRequest(String id, int timeout) throws SEPAPropertiesException, SEPASecurityException {
		HTTPMethod method = properties.getUpdateMethod(id);
		String scheme = properties.getUpdateProtocolScheme(id);
		String host = properties.getUpdateHost(id);
		int port = properties.getUpdatePort(id);
		String path = properties.getUpdatePath(id);
		String sparql = properties.getSPARQLUpdate(id);
		String graphUri = properties.getUsingGraphURI(id);
		String namedGraphUri = properties.getUsingNamedGraphURI(id);

		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();
		
		return new UpdateRequest(method, scheme, host, port, path, sparql, timeout, graphUri, namedGraphUri,
				authorization);
	}

	protected static QueryRequest buildQueryRequest(String id, int timeout) throws SEPAPropertiesException, SEPASecurityException {
		HTTPMethod method = properties.getQueryMethod(id);
		String scheme = properties.getQueryProtocolScheme(id);
		String host = properties.getQueryHost(id);
		int port = properties.getQueryPort(id);
		String path = properties.getQueryPath(id);
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();
		
		return new QueryRequest(method, scheme, host, port, path, sparql, timeout, graphUri, namedGraphUri,
				authorization);
	}

	protected static SubscribeRequest buildSubscribeRequest(String id) throws SEPAPropertiesException, SEPASecurityException {
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);
		
		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();

		return new SubscribeRequest(sparql, null, graphUri, namedGraphUri, authorization);
	}
	
	private UnsubscribeRequest buildUnsubscribeRequest(String spuid) throws SEPAPropertiesException, SEPASecurityException {
		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();
		
		return new UnsubscribeRequest(spuid, authorization);
	}
}