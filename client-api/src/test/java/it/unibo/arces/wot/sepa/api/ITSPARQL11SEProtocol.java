package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.logging.Logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;

public class ITSPARQL11SEProtocol {
	private static ConfigurationProvider provider;
	private static Sync handler;

	private static SPARQL11SEProtocol client;
	private static SubscriptionProtocol protocol;

	@BeforeEach
	@Timeout(5)
	public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			URISyntaxException, InterruptedException {
		provider = new ConfigurationProvider();
		
		handler = new Sync();
		
		protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
				provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(),handler,
				provider.getClientSecurityManager());
		
		client = new SPARQL11SEProtocol(protocol,provider.getClientSecurityManager());
		
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@AfterEach
	public void endTest() throws IOException, InterruptedException, SEPAProtocolException {		
		client.close();
		protocol.close();
		protocol.close();
		
		Thread.sleep(ConfigurationProvider.SLEEP);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void DeleteAllWithCheck() throws SEPAPropertiesException, SEPASecurityException, InterruptedException {
		// Delete all triples
		Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
		Logging.logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		// Evaluate if the store is empty
		ret = client.query(provider.buildQueryRequest("COUNT"));
		Logging.logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		QueryResponse results = (QueryResponse) ret;
		Logging.logger.debug(ret);
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
		Logging.logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void MalformedUpdate()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("WRONG"));
		Logging.logger.debug(ret);
		assertTrue(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void Query() throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("ALL"));
		Logging.logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void MalformedQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.query(provider.buildQueryRequest("WRONG"));
		Logging.logger.debug(ret);
		assertTrue(ret.isError(),String.valueOf(ret));
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void UpdateAndQuery()
			throws IOException, SEPAPropertiesException, SEPASecurityException, InterruptedException {
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		Logging.logger.debug(ret);
		assertFalse(ret.isError(),String.valueOf(ret));

		ret = client.query(provider.buildQueryRequest("VAIMEE"));
		Logging.logger.debug(ret);
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
		
		Response ret = client.update(provider.buildUpdateRequest("VAIMEE"));
		assertFalse(ret.isError(),ret.toString());
		
		handler.waitEvents(2);

		assertFalse(handler.getEvents() != 2,"Events:" + handler.getEvents() + "(2)");
		assertFalse(handler.getSubscribes() != 1,"Subscribes:" + handler.getSubscribes() + "(1)");
	}
}