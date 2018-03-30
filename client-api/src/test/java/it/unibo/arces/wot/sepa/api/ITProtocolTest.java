package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class ITProtocolTest {

    private SPARQL11SEProtocol client;
    private MockSubscriptionHandler subHandler;

    @Before
    public void setUp() throws Exception {
        final SPARQL11SEProperties properties = ConfigurationProvider.GetTestEnvConfiguration();
        subHandler = new MockSubscriptionHandler();
        client = new SPARQL11SEProtocol(properties,subHandler);
    }

    @After
    public void tearDown() throws IOException {
        if(client != null) {
            client.close();
        }
    }

    @Test(timeout=5000)
    public void Update(){
        final Response update = SubmitUpdate(client,TestQueries.SIMPLE_UPDATE);
        assertFalse(String.valueOf(update.getAsJsonObject()),update.isError());
    }

    @Test(timeout=5000)
    public void Query(){
        final Response response = SubmitQuery(client, TestQueries.SIMPLE_QUERY);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
    }

    @Test(timeout=5000)
    public void Subscribe(){
        final Response response = submitSubscribe(TestQueries.SIMPLE_QUERY, client);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
    }

    @Test(timeout=5000)
    public void Ping() throws InterruptedException {
        final Response response = submitSubscribe(TestQueries.SIMPLE_QUERY, client);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
        assertTrue(subHandler.pingRecived());
    }

    @Test(timeout=20000)
    public void NotificationTest() throws InterruptedException {
        final Response response = submitSubscribe(TestQueries.NOTIF_QUERY, client);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());

        final Response update = SubmitUpdate(client,TestQueries.NOTIF_UPDATE);
        assertFalse(String.valueOf(update.getAsJsonObject()),update.isError());

        final Response notification = subHandler.getResponse();
        assertFalse(String.valueOf(notification.getAsJsonObject()),notification.isError());


    }

    @Test
    public void VerifiedUTF8Update(){
        Update();

        final Response response = SubmitQuery(client, TestQueries.UTF8_RESULT_QUERY);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());

        QueryResponse queryResponse = (QueryResponse) response;
        List<Bindings> results = queryResponse.getBindingsResults().getBindings();
        assertTrue("Query results empty",results.size() > 0 );

        Bindings bindings = results.get(0);
        assertTrue("Binding variable is not a literal",bindings.isLiteral("o"));

        String value = bindings.getBindingValue("o");
        assertEquals("Incorrect utf-8 value","測試",value);
    }

    private static Response SubmitQuery(SPARQL11SEProtocol client,String query) {
        final QueryRequest queryRequest = new QueryRequest(query);
        return client.query(queryRequest);
    }

    private static Response SubmitUpdate(SPARQL11SEProtocol client,String query) {
        final UpdateRequest updateRequest = new UpdateRequest(query);
        return client.update(updateRequest);
    }

    private static Response submitSubscribe(String query, SPARQL11SEProtocol client) {
        SubscribeRequest sub = new SubscribeRequest(query);
        return client.subscribe(sub);
    }
}