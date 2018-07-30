package it.unibo.arces.wot.sepa.api.protocols.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebsocketSubscriptionProtocol extends SubscriptionProtocol {
	protected final Logger logger = LogManager.getLogger();

	private final SEPAWebsocketClient client;

	public WebsocketSubscriptionProtocol(String host, int port, String path, SEPASecurityManager sm,
			ISubscriptionHandler handler) throws SEPAProtocolException, SEPASecurityException {
		super(handler,sm);

		try {
			client = new SEPAWebsocketClient(new URI("wss://" + host + ":" + port + path), handler, sm.getSSLSocket());
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}			
	}

	public WebsocketSubscriptionProtocol(String host, String path, SEPASecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException, SEPASecurityException {
		super(handler,sm);

		try {
			client = new SEPAWebsocketClient(new URI("wss://" + host + path), handler, sm.getSSLSocket());
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}	
	}

	public WebsocketSubscriptionProtocol(String host, int port, String path, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(handler,null);

		try {
			client = new SEPAWebsocketClient(new URI("ws://" + host + ":" + port + path), handler);
		} catch (URISyntaxException  e) {
			throw new SEPAProtocolException(e);
		}	
	}

	public WebsocketSubscriptionProtocol(String host, String path, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(handler,null);
		
		try {
			client = new SEPAWebsocketClient(new URI("ws://" + host + path), handler);
		} catch (URISyntaxException  e) {
			throw new SEPAProtocolException(e);
		}	
	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		logger.debug("@subscribe: "+request);
		if (!client.isOpen()) {
			logger.debug("connectBlocking...");
			try {
				if(client.connectBlocking()) logger.debug("Connected");;
			} catch (InterruptedException e1) {
				throw new SEPAProtocolException(e1);
			}
		}
		
		try {
			logger.debug("Send");
			client.send(request.toString());
		} catch (NotYetConnectedException e) {
			logger.error(e.getMessage());
			throw new SEPAProtocolException(e);
		}
	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		logger.debug("@unsubscribe: "+request);
		try {
			client.send(request.toString());
		} catch (NotYetConnectedException e) {
			logger.error(e.getMessage());
			throw new SEPAProtocolException(e);
		}
	}

	@Override
	public void close() {
		logger.debug("Close");
		client.close();
	}

}
