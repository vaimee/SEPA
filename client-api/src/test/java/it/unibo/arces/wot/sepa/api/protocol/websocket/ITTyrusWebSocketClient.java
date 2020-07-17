package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import static org.junit.Assert.assertFalse;

public class ITTyrusWebSocketClient {
	protected final Logger logger = LogManager.getLogger();
	protected static JSAP properties = null;
	
	protected static String url = null;
	protected static Sync sync;
	protected static ConfigurationProvider provider = null;

	@BeforeClass
	public static void init() {	 
		try {
			provider = new ConfigurationProvider();
			properties = provider.getJsap();
			sync = new Sync();
		} catch (SEPAPropertiesException | SEPASecurityException  e) {
			assertFalse("Configuration not found", false);
		}
		if (properties.isSecure()) {
			int port = properties.getSubscribePort();
			if (port == -1)
				url = "wss://" + properties.getSubscribeHost() + properties.getSubscribePath();
			else
				url = "wss://" + properties.getSubscribeHost() + ":" + String.valueOf(port)
						+ properties.getSubscribePath();
		} else {
			int port = properties.getSubscribePort();
			if (port == -1)
				url = "ws://" + properties.getSubscribeHost() + properties.getSubscribePath();
			else
				url = "ws://" + properties.getSubscribeHost() + ":" + String.valueOf(port)
						+ properties.getSubscribePath();
		}
	}

	@Test (timeout = 10000)
	public void Connect() throws URISyntaxException, SEPASecurityException, DeploymentException, IOException {
		int n = 100;
		
		sync.reset();

		for (int i = 0; i < n; i++) {
			ClientManager client = ClientManager.createClient();
			if (properties.isSecure()) {
				SslEngineConfigurator config = new SslEngineConfigurator(provider.getSecurityManager().getSSLContext());
				config.setHostVerificationEnabled(false);
				client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, config);	
			}
			client.connectToServer(new TyrusWebsocketClient(sync), new URI(url));
		}

		sync.waitConnections(n);
	}

}
