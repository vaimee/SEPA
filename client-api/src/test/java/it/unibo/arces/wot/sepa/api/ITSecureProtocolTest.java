package it.unibo.arces.wot.sepa.api;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.AuthenticationProperties;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import static org.junit.Assert.*;

public class ITSecureProtocolTest extends ITProtocolTest {
	private final static String VALID_ID = "SEPATest";
	private final static String NOT_VALID_ID = "RegisterMePlease";

	private static SEPASecurityManager sm;
	
	@BeforeClass
	public static void init() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();

		sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",
				new AuthenticationProperties(properties.getFileName()));

		// Registration
		Response response = sm.register(NOT_VALID_ID);
		assertFalse("Failed to register a not valid ID", !response.isError());
		response = sm.register(VALID_ID);
		assertFalse("Failed to register a valid ID", response.isError());	
		
		for (String id : properties.getQueryIds()) {
			queries.put(id, getQueryRequest(id, 5000, sm.getAuthorizationHeader()));
			subscribes.put(id, getSubscribeRequest(id, sm.getAuthorizationHeader()));
			protocols.put(id, new WebSocketSubscriptionProtocol(properties.getSubscribeHost(id),
					properties.getSubscribePort(id), properties.getSubscribePath(id),sm));
		}

		for (String id : properties.getUpdateIds()) {
			updates.put(id, getUpdateRequest(id, 5000, sm.getAuthorizationHeader()));
		}
	}

	@Before
	public void beginTest() throws IOException, IllegalArgumentException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		client = new SPARQL11Protocol(sm);
		seClient = new SPARQL11SEProtocol(protocols.get("Q2"), subHandler,sm);
		
		// Set authorization header
		for (UpdateRequest req : updates.values()) req.setAuthorizationHeader(sm.getAuthorizationHeader());
		for (QueryRequest req : queries.values()) req.setAuthorizationHeader(sm.getAuthorizationHeader());
		for (SubscribeRequest req : subscribes.values()) req.setAuthorizationHeader(sm.getAuthorizationHeader());
		
		final Response ret = client.update(updates.get("DELETE_ALL"));
		
		assertFalse(String.valueOf(ret), ret.isError());
	}
}
