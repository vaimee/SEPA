package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

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
    private JSAP properties;

    @Before
    public void setUp() throws Exception {
        URL config = Thread.currentThread().getContextClassLoader().getResource("dev.jsap");
        properties = new JSAP(config.getPath());
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
		client = new SPARQL11SEProtocol(protocol, subHandler);
    }

    @Test
    public void Register() throws SEPASecurityException{
        Response response;
        response = new SEPASecurityManager().register(properties.getAuthenticationProperties().getRegisterUrl(),NOT_VALID_ID);
        assertTrue("Accepted not valid ID",response.isError());

        response = new SEPASecurityManager().register(properties.getAuthenticationProperties().getRegisterUrl(),VALID_ID);
        assertTrue("Not accepted valid ID",response.isError());
    }

    @Test
    @Ignore
    public void RequestToken() throws SEPASecurityException {
        Response response;
        response = new SEPASecurityManager().requestToken(properties.getAuthenticationProperties().getTokenRequestUrl(),properties.getAuthenticationProperties().getBasicAuthorizationHeader());
        assertFalse(String.valueOf(response),response.isError());

        assertFalse("SEPA returned an expired token",properties.getAuthenticationProperties().isTokenExpired());
    }
}
