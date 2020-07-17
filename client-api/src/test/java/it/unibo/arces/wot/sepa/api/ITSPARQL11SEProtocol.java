package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Publisher;
import it.unibo.arces.wot.sepa.Subscriber;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
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

import static org.junit.Assert.*;

public class ITSPARQL11SEProtocol {
	protected final Logger logger = LogManager.getLogger();
	
	private static JSAP properties = null;
	private static ConfigurationProvider provider;

	//private static ClientSecurityManager sm;
	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private static Sync sync;

	private static SPARQL11Protocol client;
	private static Subscriber subscriber;
	private static Publisher publisher;
	
	@BeforeClass
	public static void init() throws Exception {
		provider = new ConfigurationProvider();
		properties = provider.getJsap();
		sync = new Sync();
	}

	@Before
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {

		sync.reset();

		client = new SPARQL11Protocol(provider.getSecurityManager());
		subscriber = new Subscriber("VAIMEE", sync);
		publisher = new Publisher("VAIMEE", 1);

		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError() && properties.isSecure()) provider.getSecurityManager().refreshToken();
			ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		}
		
		logger.debug(ret);

		assertFalse(String.valueOf(ret), ret.isError());
	}

	@After
	public void endTest() throws IOException, InterruptedException, SEPAProtocolException {	
		subscriber.close();
		publisher.close();		
		client.close();
		sync.close();
	}

	@Test(timeout = 5000)
	public void RegisterNotAllowed() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = provider.getSecurityManager().register(NOT_VALID_ID);
			logger.debug(response);
			assertFalse(response.toString(), !response.isError());
		}
	}

	@Test(timeout = 5000)
	public void Register() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = provider.getSecurityManager().register(VALID_ID);
			logger.debug(response);
			assertFalse(response.toString(), response.isError());
		}
	}

	@Test(timeout = 5000)
	public void DeleteAllWithCheck() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		// Delete all triples
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		logger.debug(ret);
		if (ret.isError()) {
			ErrorResponse err = (ErrorResponse) ret;
			if (err.isTokenExpiredError()) provider.getSecurityManager().refreshToken();
			else assertFalse(String.valueOf(ret), ret.isError());
			ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		}
		assertFalse(String.valueOf(ret), ret.isError());

		// Evaluate if the store is empty
		ret = client.query(provider.buildQueryRequest("COUNT"));
		logger.debug(ret);
		assertFalse(String.valueOf(ret), ret.isError());

		QueryResponse results = (QueryResponse) ret;
		logger.debug(ret);
		assertFalse(String.valueOf(results), results.getBindingsResults().size() != 1);

		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse("Results are null " + String.valueOf(results), bindings.getValue("n") == null);
			assertFalse("RDF store is not empty " + String.valueOf(results), !bindings.getValue("n").equals("0"));
		}
	}

	@Test(timeout = 15000)
	public void UseExpiredToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (properties.isSecure()) {
			String authorization = provider.getSecurityManager().getAuthorizationHeader();

			assertFalse("Failed to get authorization header", authorization == null);

			final long expiringTime = 5000;
			Thread.sleep(expiringTime + 1000);
			final Response tokenTest = client.query(provider.buildQueryRequest("ALL",authorization));
			logger.debug(tokenTest);
			assertTrue(tokenTest.toString(), tokenTest.isError());
		}
	}

	@Test(timeout = 1000)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 1000)
	public void MalformedUpdate()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("WRONG"));
		logger.debug(ret);
		assertTrue(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("ALL"));
		logger.debug(ret);
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void MalformedQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("WRONG"));
		logger.debug(ret);
		assertTrue(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void UpdateAndQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(String.valueOf(ret), ret.isError());

		ret = client.query(provider.buildQueryRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(String.valueOf(ret), ret.isError());

		assertFalse(String.valueOf(ret), ((QueryResponse) ret).getBindingsResults().size() != 1);
	}

	@Test(timeout = 5000)
	public void Subscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {

		subscriber.start();

		sync.waitSubscribes(1);
		sync.waitEvents(1);
		
		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + 1 + ")",
				sync.getSubscribes() != 1);
		assertFalse("Events:" + sync.getEvents() + "(" + 1 + ")",
				sync.getEvents() != 1);	
	}

	@Test(timeout = 5000)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, InterruptedException {
		
		subscriber.start();

		sync.waitSubscribes(1);
		
		publisher.start();

		sync.waitEvents(2);

		assertFalse("Subscribes:" + sync.getSubscribes() + "(" + 1 + ")",
				sync.getSubscribes() != 1);
		assertFalse("Events:" + sync.getEvents() + "(2)", sync.getEvents() != 2);
	}
}