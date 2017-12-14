package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class ITProtocolTest {

    private SPARQL11SEProtocol sparql11SEProtocol;

    @Before
    public void setUp() throws Exception {
        URL config = Thread.currentThread().getContextClassLoader().getResource("testenvironment.jsap");
        final SPARQL11SEProperties properties = new SPARQL11SEProperties(new File(config.getPath()));
        sparql11SEProtocol = new SPARQL11SEProtocol(properties);
    }

    @Test
    public void UpdateAndQuery(){
        final UpdateRequest updateRequest = new UpdateRequest("INSERT DATA { <http://javatest> <http://javatest> <http://javatest> }");
        final Response update = sparql11SEProtocol.update(updateRequest);

        assertFalse(String.valueOf(update.getAsJsonObject()),update.isError());

        final QueryRequest queryRequest = new QueryRequest("SELECT ?o WHERE {<http://javatest> <http://javatest> ?o} ");
        final Response query = sparql11SEProtocol.query(queryRequest);

        assertFalse(String.valueOf(query.getAsJsonObject()),query.isError());

    }
}