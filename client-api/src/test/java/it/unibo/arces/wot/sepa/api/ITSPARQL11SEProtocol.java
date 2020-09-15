package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;

public class ITSPARQL11SEProtocol {
	protected static final Logger logger = LogManager.getLogger();
	
	private static JSAP properties = null;
	private static ConfigurationProvider provider;
	private static ClientSecurityManager sm;
	private static Sync handler;

	private final static String NOT_VALID_ID = "RegisterMePlease";

	private static SPARQL11SEProtocol client;
	
	@BeforeAll
	public static void init() throws Exception {
		provider = new ConfigurationProvider();
		properties = provider.getJsap();
		sm = provider.getSecurityManager();
		handler = new Sync(sm);
		
	}
	
	@AfterAll
	public static void end() throws IOException {
		logger.debug("end");
		//client.close();
	}

	@BeforeEach
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {
		
		client = new SPARQL11SEProtocol(new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(),handler,
				sm),sm);
		
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		
		if (ret.isError()) {
			ErrorResponse error = (ErrorResponse) ret;
			if (error.isTokenExpiredError() && properties.isSecure()) {
				provider.getSecurityManager().refreshToken();
				ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
				if (ret.isError()) assertFalse(true,ret.toString());
			}
		}
		
		logger.debug(ret);

		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@AfterEach
	public void endTest() throws IOException, InterruptedException, SEPAProtocolException {		
		client.close();
	}

	//@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(5)
	public void RegisterNotAllowed() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = sm.register(NOT_VALID_ID);
			logger.debug(response);
			assertFalse(!response.isError(),response.toString());
		}
	}

	//@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(5)
	public void Register() throws SEPASecurityException, SEPAPropertiesException {
		if (properties.isSecure()) {
			Response response = sm.register(provider.getClientId());
			logger.debug(response);
			assertFalse(response.isError(),response.toString());
		}
	}
	
	//@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(10)
	public void UseExpiredToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
		if (properties.isSecure()) {
			String authorization = sm.getAuthorizationHeader();

			assertFalse(authorization == null,"Failed to get authorization header" );

			final long expiringTime = 5000;
			Thread.sleep(expiringTime + 1000);
			final Response tokenTest = client.query(provider.buildQueryRequest("ALL",authorization));
			logger.debug(tokenTest);
			assertTrue(tokenTest.isError(),tokenTest.toString());
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void DeleteAllWithCheck() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		// Delete all triples
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		logger.debug(ret);
		if (ret.isError()) {
			ErrorResponse err = (ErrorResponse) ret;
			if (err.isTokenExpiredError()) provider.getSecurityManager().refreshToken();
			else assertFalse(ret.isError(),String.valueOf(ret) );
			ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		}
		assertFalse(ret.isError(),String.valueOf(ret));

		// Evaluate if the store is empty
		ret = client.query(provider.buildQueryRequest("COUNT"));
		logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		QueryResponse results = (QueryResponse) ret;
		logger.debug(ret);
		assertFalse(results.getBindingsResults().size() != 1,String.valueOf(results));

		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			assertFalse(bindings.getValue("n") == null,"Results are null " + String.valueOf(results));
			assertFalse(!bindings.getValue("n").equals("0"),"RDF store is not empty " + String.valueOf(results));
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void Update() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void MalformedUpdate()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("WRONG"));
		logger.debug(ret);
		assertTrue(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("ALL"));
		logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void MalformedQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("WRONG"));
		logger.debug(ret);
		assertTrue(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void UpdateAndQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		ret = client.query(provider.buildQueryRequest("VAIMEE"));
		logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		assertFalse(((QueryResponse) ret).getBindingsResults().size() != 1,String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void Subscribe()
			throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException, IOException {
		handler.reset();
		assertFalse(handler.getSubscribes() != 0,"Subscribes:" + handler.getSubscribes() + "(" + 0 + ")");
		assertFalse(handler.getEvents() != 0,"Events:" + handler.getEvents() + "(" + 0 + ")");
		
		client.subscribe(provider.buildSubscribeRequest("VAIMEE"));

		handler.waitSubscribes(1);
		handler.waitEvents(1);
		
		assertFalse(handler.getSubscribes() != 1,"Subscribes:" + handler.getSubscribes() + "(" + 1 + ")");
		assertFalse(handler.getEvents() != 1,"Events:" + handler.getEvents() + "(" + 1 + ")");
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void Notify() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, InterruptedException {
		handler.reset();
		assertFalse(handler.getSubscribes() != 0,"Subscribes:" + handler.getSubscribes() + "(" + 0 + ")");
		assertFalse(handler.getEvents() != 0,"Events:" + handler.getEvents() + "(" + 0 + ")");
		
		client.subscribe(provider.buildSubscribeRequest("VAIMEE"));

		handler.waitSubscribes(1);
		handler.waitEvents(1);
		
		client.update(provider.buildUpdateRequest("VAIMEE"));

		handler.waitEvents(2);

		assertFalse(handler.getEvents() != 2,"Events:" + handler.getEvents() + "(2)");
		assertFalse(handler.getSubscribes() != 1,"Subscribes:" + handler.getSubscribes() + "(1)");
	}
}