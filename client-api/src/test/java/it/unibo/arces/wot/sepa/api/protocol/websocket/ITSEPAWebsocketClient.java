package it.unibo.arces.wot.sepa.api.protocol.websocket;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;
//import org.junit.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.JavaWebsocketClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ITSEPAWebsocketClient implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();
	protected static JSAP properties = null;
	protected static String url = null;
	
	JavaWebsocketClient client = null;
	
	@BeforeClass
	public static void init() throws Exception {
		properties = new ConfigurationProvider().getJsap();
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

	//@Test(timeout = 10000)
	public void Connect() throws InterruptedException, URISyntaxException, IOException, SEPASecurityException {
		for (int i = 0; i < 100; i++) {
			if(properties.isSecure()) {
				SEPASecurityManager sm = new SEPASecurityManager();
				client = new JavaWebsocketClient(new URI(url), this,
						sm.getSSLSocket());
			}
			else {
				client = new JavaWebsocketClient(new URI(url), this);
			}
			
			assertFalse("Failed to connect", !client.connectBlocking());
			
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("@onSemanticEvent: " + notify);
	}

	@Override
	public void onBrokenConnection() {
		logger.debug("@onBrokenConnection");
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.debug("@onError: " + errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("@onSubscribe: " + spuid + " alias: " + alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("@onUnsubscribe: " + spuid);
	}
}
