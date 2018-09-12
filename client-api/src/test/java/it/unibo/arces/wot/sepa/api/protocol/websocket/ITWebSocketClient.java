package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.api.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import static org.junit.Assert.assertFalse;

public class ITWebSocketClient {
	protected final Logger logger = LogManager.getLogger();
	protected static JSAP properties = null;
	protected static String url = null;
	
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
	
	@BeforeClass
	public static void init() throws Exception {
		properties = ConfigurationProvider.GetTestEnvConfiguration();
		if(properties.isSecure()) {
			int port = properties.getSubscribePort();
			if (port == -1)
				url = "wss://"+properties.getDefaultHost()+properties.getSubscribePath();
			else
				url = "wss://"+properties.getDefaultHost()+":"+String.valueOf(port)+properties.getSubscribePath();
		}
		else {
			int port = properties.getSubscribePort();
			if (port == -1)
				url = "ws://"+properties.getDefaultHost()+properties.getSubscribePath();
			else
				url = "ws://"+properties.getDefaultHost()+":"+String.valueOf(port)+properties.getSubscribePath();
		}
	}
	
	@Test(timeout=20000)
	public void Connect() throws URISyntaxException, InterruptedException, SEPASecurityException, IOException {
		
		
		for (int i=0; i < 100; i++) {
			WebsocketTest client = new WebsocketTest(new URI(url));
			
			if (properties.isSecure()) {
				SEPASecurityManager sm = new SEPASecurityManager();
				client.setSocket(sm.getSSLSocket());
			}
			
			assertFalse("Failed to connect",!client.connectBlocking());
		}
	}
	
}
