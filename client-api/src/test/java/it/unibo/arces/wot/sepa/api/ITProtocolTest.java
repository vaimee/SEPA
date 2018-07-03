package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ITProtocolTest {

	private static HashMap<String, UpdateRequest> updates = new HashMap<String, UpdateRequest>();
	private static HashMap<String, QueryRequest> queries = new HashMap<String, QueryRequest>();
	private static HashMap<String, SubscribeRequest> subscribes = new HashMap<String, SubscribeRequest>();
	private static HashMap<String, ISubscriptionProtocol> protocols = new HashMap<String, ISubscriptionProtocol>();

	private static JSAP properties = null;

	private static final MockSubscriptionHandler subHandler = new MockSubscriptionHandler();;

	private static SPARQL11Protocol client = null;

	@BeforeClass
	public static void start() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();

		for (String id : properties.getQueryIds()) {
			queries.put(id, getQueryRequest(id, 5000, null));
			subscribes.put(id, getSubscribeRequest(id, null));
			protocols.put(id, new WebSocketSubscriptionProtocol(properties.getSubscribeHost(id),
					properties.getSubscribePort(id), properties.getSubscribePath(id)));
		}

		for (String id : properties.getUpdateIds()) {
			updates.put(id, getUpdateRequest(id, 5000, null));
		}
	}

	@AfterClass
	public static void stop() throws IOException {

	}

	@Before
	public void beginTest() throws IOException {
		client = new SPARQL11Protocol();
		final Response ret = client.update(updates.get("DELETE_ALL"));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@After
	public void endTest() throws IOException {
		if (client != null) {
			client.close();
		}
	}

	@Test(timeout = 5000)
	public void Update() throws IOException {
		client = new SPARQL11Protocol();
		final Response ret = client.update(updates.get("U1"));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void Query() throws IOException {
		client = new SPARQL11Protocol();
		final Response ret = client.query(queries.get("Q1"));
		assertFalse(String.valueOf(ret), ret.isError());
	}

	@Test(timeout = 5000)
	public void UpdateAndQueryOneResult() throws IOException {
		client = new SPARQL11Protocol();
		final Response updateRes = client.update(updates.get("U2"));
		final Response queryRes = client.query(queries.get("Q2"));

		if (updateRes.isError())
			assertFalse("Update: " + String.valueOf(updateRes), true);
		else if (queryRes.isError())
			assertFalse(" Query: " + String.valueOf(queryRes), true);
		else
			assertFalse(String.valueOf(queryRes), ((QueryResponse) queryRes).getBindingsResults().size() != 1);
	}

	@Test(timeout = 5000)
	public void SubscribeAndNotifyOneAddedResult()
			throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException {
		client = new SPARQL11SEProtocol(protocols.get("Q2"), subHandler);

		final Response subRes = ((SPARQL11SEProtocol) client).subscribe(subscribes.get("Q2"));
		final Response updateRes = client.update(updates.get("U2"));

		Response notify = subHandler.getResponse();

		if (subRes.isError())
			assertFalse(" Subscribe: " + String.valueOf(subRes), true);
		else if (updateRes.isError())
			assertFalse("Update: " + String.valueOf(updateRes), true);
		else if (notify.isError())
			assertFalse(String.valueOf(notify), true);
		else if (((Notification)notify).getARBindingsResults().getAddedBindings().size() != 1) assertFalse(String.valueOf(notify), true);
			
	}

	private static UpdateRequest getUpdateRequest(String id, int timeout, String authorization) {
		HTTPMethod method = properties.getUpdateMethod(id);
		String scheme = properties.getUpdateProtocolScheme(id);
		String host = properties.getUpdateHost(id);
		int port = properties.getUpdatePort(id);
		String path = properties.getUpdatePath(id);
		String sparql = properties.getSPARQLUpdate(id);
		String graphUri = properties.getUsingGraphURI(id);
		String namedGraphUri = properties.getUsingNamedGraphURI(id);

		return new UpdateRequest(method, scheme, host, port, path, sparql, timeout, graphUri, namedGraphUri,
				authorization);
	}

	private static QueryRequest getQueryRequest(String id, int timeout, String authorization) {
		HTTPMethod method = properties.getQueryMethod(id);
		String scheme = properties.getQueryProtocolScheme(id);
		String host = properties.getQueryHost(id);
		int port = properties.getQueryPort(id);
		String path = properties.getQueryPath(id);
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		return new QueryRequest(method, scheme, host, port, path, sparql, timeout, graphUri, namedGraphUri,
				authorization);
	}

	private static SubscribeRequest getSubscribeRequest(String id, String authorization) {
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		return new SubscribeRequest(sparql, null, graphUri, namedGraphUri, authorization);
	}
}