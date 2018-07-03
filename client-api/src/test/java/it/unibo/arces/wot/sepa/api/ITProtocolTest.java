package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ITProtocolTest {

	// private SPARQL11SEProtocol client;
	// private MockSubscriptionHandler subHandler;
	private static JSAP properties = null;

	@Before
	public void setUp() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();
		// subHandler = new MockSubscriptionHandler();
		//
		// ISubscriptionProtocol protocol = null;
		// protocol = new
		// WebSocketSubscriptionProtocol(properties.getSubscribeHost(null),
		// properties.getSubscribePort(null), properties.getSubscribePath(null));
		//
		// client = new SPARQL11SEProtocol(protocol, subHandler);
	}

	@After
	public void tearDown() throws IOException {
		// if(client != null) {
		// client.close();
		// }
	}

	@Test(timeout=5000)
    public void UpdateTest() throws IOException{
    		SPARQL11Protocol client = new SPARQL11Protocol();   		
    		UpdateRequest req = getUpdateRequest("U1",5000,null);
    		final Response ret = client.update(req);
    		client.close();
        assertFalse(String.valueOf(ret),ret.isError());
    }
	
	@Test(timeout=5000)
    public void QueryTest() throws IOException{
    		SPARQL11Protocol client = new SPARQL11Protocol();   		
    		QueryRequest req = getQueryRequest("Q1",5000,null);
    		final Response ret = client.query(req);
    		client.close();
        assertFalse(String.valueOf(ret),ret.isError());
    }

	private static UpdateRequest getUpdateRequest(String id,int timeout,String authorization) {	
		HTTPMethod method = properties.getUpdateMethod(id);
		String scheme = properties.getUpdateProtocolScheme(id);
		String host = properties.getUpdateHost(id);
		int port = properties.getUpdatePort(id);
		String path = properties.getUpdatePath(id);
		String sparql = properties.getSPARQLUpdate(id);
		String graphUri = properties.getUsingGraphURI(id);
		String namedGraphUri = properties.getUsingNamedGraphURI(id);
		
		return new UpdateRequest( method,  scheme,  host,  port, path,  sparql,  timeout,  graphUri,  namedGraphUri, authorization);
    }
	
	private static QueryRequest getQueryRequest(String id,int timeout,String authorization) {	
		HTTPMethod method = properties.getQueryMethod(id);
		String scheme = properties.getQueryProtocolScheme(id);
		String host = properties.getQueryHost(id);
		int port = properties.getQueryPort(id);
		String path = properties.getQueryPath(id);
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);
		
		return new QueryRequest( method,  scheme,  host,  port, path,  sparql,  timeout,  graphUri,  namedGraphUri, authorization);
    }

	// @Test(timeout=5000)
	// public void Update(){
	// final Response update = SubmitUpdate(client,TestQueries.SIMPLE_UPDATE);
	// assertFalse(String.valueOf(update),update.isError());
	// }
	//
	// @Test(timeout=5000)
	// public void Query(){
	// final Response response = SubmitQuery(client, TestQueries.SIMPLE_QUERY);
	// assertFalse(String.valueOf(response),response.isError());
	// }
	//
	// @Test(timeout=5000)
	// public void Subscribe(){
	// final Response response = submitSubscribe(TestQueries.SIMPLE_QUERY, client);
	// assertFalse(String.valueOf(response),response.isError());
	// }
	//
	// @Test(timeout=20000)
	// public void NotificationTest() throws InterruptedException {
	// final Response response = submitSubscribe(TestQueries.NOTIF_QUERY, client);
	// assertFalse(String.valueOf(response),response.isError());
	//
	// final Response update = SubmitUpdate(client,TestQueries.NOTIF_UPDATE);
	// assertFalse(String.valueOf(update),update.isError());
	//
	// final Response notification = subHandler.getResponse();
	// assertFalse(String.valueOf(notification),notification.isError());
	// }
	//
	// @Test
	// public void VerifiedUTF8Update(){
	// Update();
	//
	// final Response response = SubmitQuery(client, TestQueries.UTF8_RESULT_QUERY);
	// assertFalse(String.valueOf(response),response.isError());
	//
	// QueryResponse queryResponse = (QueryResponse) response;
	// List<Bindings> results = queryResponse.getBindingsResults().getBindings();
	// assertTrue("Query results empty",results.size() > 0 );
	//
	// Bindings bindings = results.get(0);
	// assertTrue("Binding variable is not a literal",bindings.isLiteral("o"));
	//
	// String value = bindings.getValue("o");
	// assertEquals("Incorrect utf-8 value","測試",value);
	// }
	//
	// private static Response SubmitQuery(SPARQL11SEProtocol client,String query) {
	// final QueryRequest queryRequest = new QueryRequest(query);
	//
	// return client.query(queryRequest);
	// }
	//
	// private static Response SubmitUpdate(SPARQL11SEProtocol client,String query)
	// {
	// final UpdateRequest updateRequest = new UpdateRequest(query);
	// return client.update(updateRequest);
	// }
	//
	// private static Response submitSubscribe(String query, SPARQL11SEProtocol
	// client) {
	// SubscribeRequest sub = new SubscribeRequest(query,null,null,null,null);
	// return client.subscribe(sub);
	// }
}