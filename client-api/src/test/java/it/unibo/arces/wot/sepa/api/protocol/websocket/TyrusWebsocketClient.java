package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

class TyrusWebsocketClient extends Endpoint implements Closeable {
	protected Logger logger = LogManager.getLogger();

	protected final Sync sync;
	protected Session session;
	protected static ClientManager client;

	public TyrusWebsocketClient(Sync s,ConfigurationProvider provider) throws SEPASecurityException {
		sync = s;

		client = ClientManager.createClient();

		if (provider.getJsap().isSecure()) {
			SslEngineConfigurator config = new SslEngineConfigurator(provider.getSecurityManager().getSSLContext());
			config.setHostVerificationEnabled(false);
			client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, config);
		}
	}

	@Override
	public void onOpen(Session session, EndpointConfig arg1) {
		logger.info("onOpen session: " + session.getId());
		this.session = session;
		sync.onConnection();
	}

	@Override
	public void close() throws IOException {
		session.close();
	
		client.shutdown();
	}
	
	public void connect(String url) throws DeploymentException, IOException, URISyntaxException {
		client.connectToServer(this, new URI(url));
	}	
}
