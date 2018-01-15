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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

public class ITProtocolTest {



    private static final String SUB_ALIAS = "!";

    //String concatenation it's really bad for the performance. But since this is a test I'd prefer code readability
    private static final String SIMPLE_UPDATE = "prefix test:<http://www.vaimee.com/test#> " +
                                                "insert data {test:Sub test:Pred \"測試\"} ";

    private static final String NOTIF_UPDATE = "prefix test:<http://www.vaimee.com/test#> " +
                                                "insert data {test:Sub test:hasNotification \"Hello there!\"} ";

    private static final String SIMPLE_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
                                                "select ?s ?p ?o " +
                                                "where {?s ?p ?o}";

    private static final String UTF8_RESULT_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
                                                "select ?s ?p ?o " +
                                                "where {test:Sub test:Pred ?o}";

    private static final String NOTIF_QUERY = "prefix test:<http://www.vaimee.com/test#> " +
                                            "select ?s ?p ?o " +
                                            "where {test:Sub test:hasNotification ?o}";

    private SPARQL11SEProtocol client;
    private MockSubscriptionHandler subHandler;

    @Before
    public void setUp() throws Exception {
        URL config = Thread.currentThread().getContextClassLoader().getResource("testenvironment.jsap");
        final SPARQL11SEProperties properties = new SPARQL11SEProperties(new File(config.getPath()));
        subHandler = new MockSubscriptionHandler();
        client = new SPARQL11SEProtocol(properties,subHandler);
    }

    @After
    public void tearDown() throws IOException {
        if(client != null) {
            client.close();
        }
    }

    @Test
    public void Update(){
        final Response update = SubmitUpdate(client,SIMPLE_UPDATE);
        assertFalse(String.valueOf(update.getAsJsonObject()),update.isError());
    }

    @Test
    public void Query(){
        final Response response = SubmitQuery(client, SIMPLE_QUERY);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
    }

    @Test
    public void Subscribe(){
        final Response response = submitSubscribe(SIMPLE_QUERY, client, SUB_ALIAS);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
    }

    @Test(timeout=5000)
    public void Ping() throws InterruptedException {
        final Response response = submitSubscribe(SIMPLE_QUERY, client, SUB_ALIAS);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());
        assertTrue(subHandler.pingRecived());
    }

    @Test(timeout=20000)
    public void NotificationTest() throws InterruptedException {
        final Response response = submitSubscribe(NOTIF_QUERY, client, SUB_ALIAS);
        assertFalse(String.valueOf(response.getAsJsonObject()),response.isError());

        final Response update = SubmitUpdate(client,NOTIF_UPDATE);
        assertFalse(String.valueOf(update.getAsJsonObject()),update.isError());

        final Response notification = subHandler.getResponse();
        assertFalse(String.valueOf(notification.getAsJsonObject()),notification.isError());


    }

    @Test
    public void VerifiedUTF8Update(){
        Update();

        final Response response = SubmitQuery(client, UTF8_RESULT_QUERY);
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

    private static Response submitSubscribe(String query, SPARQL11SEProtocol client,String alias) {
        SubscribeRequest sub = new SubscribeRequest(query);
        return client.subscribe(sub);
    }
}