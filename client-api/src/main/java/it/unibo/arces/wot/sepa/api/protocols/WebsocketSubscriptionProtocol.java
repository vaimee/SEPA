package it.unibo.arces.wot.sepa.api.protocols;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
//import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
//import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebsocketSubscriptionProtocol extends SubscriptionProtocol {
	protected final Logger logger = LogManager.getLogger();

	private SEPAWebsocketClient client = null;

	private Socket secure = null;
	private URI url;

	public WebsocketSubscriptionProtocol(String host, int port, String path, SEPASecurityManager sm,
			ISubscriptionHandler handler) throws SEPAProtocolException {
		super(handler);
		if (sm == null) {
			logger.error("Security manager is null");
			throw new IllegalArgumentException("Security manager is null");
		}

		try {
			url = new URI("wss://" + host + ":" + port + path);
		} catch (URISyntaxException e1) {
			throw new SEPAProtocolException(e1);
		}

		try {
			secure = sm.getSSLSocket();
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public WebsocketSubscriptionProtocol(String host, String path, SEPASecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(handler);
		if (sm == null) {
			logger.error("Security manager is null");
			throw new IllegalArgumentException("Security manager is null");
		}

		try {
			url = new URI("wss://" + host + path);
		} catch (URISyntaxException e1) {
			throw new SEPAProtocolException(e1);
		}

		try {
			secure = sm.getSSLSocket();
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public WebsocketSubscriptionProtocol(String host, int port, String path, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(handler);
		try {
			url = new URI("ws://" + host + ":" + port + path);
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}

	public WebsocketSubscriptionProtocol(String host, String path, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(handler);
		try {
			url = new URI("ws://" + host + path);
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}

	private void connect() throws SEPAProtocolException {
		if (client == null) {
			if (secure != null)
				client = new SEPAWebsocketClient(url, handler, secure);
			else
				client = new SEPAWebsocketClient(url, handler);

			try {
				if (!client.connectBlocking()) {
					logger.error("Failed to connect");
					throw new SEPAProtocolException("Failed to connect");
				}
			} catch (InterruptedException e) {
				throw new SEPAProtocolException(e);
			}

			logger.debug("Connected");
		}
	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		connect();

		try {
			client.send(request.toString());
		} catch (NotYetConnectedException e) {
			throw new SEPAProtocolException(e);
		}
		// try {
		// connect();
		// } catch (SEPAProtocolException e) {
		// return new ErrorResponse(500,"Failed to connect");
		// }
		// logger.debug("Subscribe: " + request.toString());
		// return client.sendAndReceive(request.toString(), request.getTimeout());
	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		try {
			client.send(request.toString());
		} catch (NotYetConnectedException e) {
			throw new SEPAProtocolException(e);
		}

		// logger.debug("Unsubscribe: " + request.toString());
		// return client.sendAndReceive(request.toString(), request.getTimeout());
	}

	@Override
	public boolean isSecure() {
		return secure != null;
	}

	@Override
	public void close() {
		if (client != null)
			client.close();
	}

}
