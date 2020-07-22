package it.unibo.arces.wot.sepa.api.protocol.websocket;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.DeploymentException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ITTyrusWebSocketClient {
	protected static final Logger logger = LogManager.getLogger();
	protected static JSAP properties = null;
	
	protected static String url = null;
	protected static Sync sync;
	protected static ConfigurationProvider provider = null;
	
	protected static Set<TyrusWebsocketClient> websockets = new HashSet<TyrusWebsocketClient>();;
	
	@BeforeAll
	public static void init() throws SEPASecurityException {	 
		try {
			provider = new ConfigurationProvider();
			properties = provider.getJsap();
			sync = new Sync();
		} catch (SEPAPropertiesException | SEPASecurityException  e) {
			assertFalse(true,"Configuration not found");
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
	
	@AfterAll
	public static void close() {
		logger.debug("end");	
	}
	
	@AfterEach
	public void endTest() throws IOException {		
		for (TyrusWebsocketClient ws : websockets) {
			ws.close();
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(15)
	public void Connect() throws URISyntaxException, DeploymentException, IOException, SEPASecurityException {
		int n = 100;
		
		sync.reset();

		for (int i = 0; i < n; i++) {
			TyrusWebsocketClient ws = new TyrusWebsocketClient(sync,provider);
			ws.connect(url);
			websockets.add(ws);
		}

		sync.waitConnections(n);
	}

}
