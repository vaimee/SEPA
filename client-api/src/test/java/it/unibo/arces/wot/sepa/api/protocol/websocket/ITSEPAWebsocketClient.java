package it.unibo.arces.wot.sepa.api.protocol.websocket;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.SEPAWebsocketClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class ITSEPAWebsocketClient implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();
	
	@Test(timeout=10000)
	public void Connect() throws InterruptedException, URISyntaxException, IOException, SEPASecurityException {
		for (int i=0; i < 100; i++) {
			SEPASecurityManager sm = new SEPASecurityManager();
			SEPAWebsocketClient client = new SEPAWebsocketClient(new URI("wss://localhost:9443/secure/subscribe"),this,sm.getSSLSocket());
			assertFalse("Failed to connect",!client.connectBlocking());
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: "+notify);		
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.debug("@onError: "+errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: "+spuid+ " alias: "+alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe: "+spuid);
	}
}
