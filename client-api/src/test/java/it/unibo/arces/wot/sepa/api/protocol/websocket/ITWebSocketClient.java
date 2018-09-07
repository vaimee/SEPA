package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import org.junit.Test;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

import static org.junit.Assert.assertFalse;

public class ITWebSocketClient {
	protected final Logger logger = LogManager.getLogger();
	
	class WebsocketTest extends WebSocketClient {

		public WebsocketTest(URI serverUri) {
			super(serverUri);
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			logger.debug("@onOpen: "+handshakedata.getHttpStatusMessage());
		}

		@Override
		public void onMessage(String message) {
			logger.debug("@onMessage: "+message);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			logger.debug("@onClose code:"+code+" reason:"+reason+" remote:"+remote);
		}

		@Override
		public void onError(Exception ex) {
			logger.error(ex.getMessage());
		}
		
	}
	
	@Test(timeout=20000)
	public void Connect() throws URISyntaxException, InterruptedException, SEPASecurityException, IOException {
		SEPASecurityManager sm = new SEPASecurityManager();
		
		for (int i=0; i < 100; i++) {
			WebsocketTest client = new WebsocketTest(new URI("wss://localhost:9443/secure/subscribe"));
			client.setSocket(sm.getSSLSocket());
			assertFalse("Failed to connect",!client.connectBlocking());
		}
	}
	
}
