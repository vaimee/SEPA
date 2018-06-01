package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import static org.junit.Assert.*;

public class ITSecureProtocolTest {

    private SPARQL11SEProtocol client;
    private MockSubscriptionHandler subHandler;

    private final static String VALID_ID = "SEPATest";
    private final static String NOT_VALID_ID = "RegisterMePlease";
    private ApplicationProfile properties;

    @Before
    public void setUp() throws Exception {
        URL config = Thread.currentThread().getContextClassLoader().getResource("dev.jsap");
        properties = new ApplicationProfile(config.getPath());
        subHandler = new MockSubscriptionHandler();
        
        ISubscriptionProtocol protocol = null;
		switch (properties.getSubscribeProtocol(null)) {
		case WS:
			protocol = new WebSocketSubscriptionProtocol(properties.getSubscribeHost(null),
					properties.getSubscribePort(null), properties.getSubscribePath(null), false);
			break;
		case WSS:
			protocol = new WebSocketSubscriptionProtocol(properties.getSubscribeHost(null),
					properties.getSubscribePort(null), properties.getSubscribePath(null), true);
			break;
		}
		client = new SPARQL11SEProtocol(properties,protocol, subHandler);
    }

    @Test
    public void Register(){
        Response response;
        response = client.register(NOT_VALID_ID);
        assertTrue("Accepted not valid ID",response.isError());

        response = client.register(VALID_ID);
        assertTrue("Not accepted valid ID",response.isError());
    }

    @Test
    @Ignore
    public void RequestToken() throws SEPASecurityException {
        Response response;
        response = client.requestToken();
        assertFalse(String.valueOf(response),response.isError());

        assertFalse("SEPA returned an expired token",properties.isTokenExpired());
    }
}
