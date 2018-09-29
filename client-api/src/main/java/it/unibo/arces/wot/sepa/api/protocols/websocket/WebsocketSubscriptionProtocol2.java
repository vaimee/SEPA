package it.unibo.arces.wot.sepa.api.protocols.websocket;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebsocketSubscriptionProtocol2 implements SubscriptionProtocol {
	public WebsocketSubscriptionProtocol2(String host, int port, String path) {

	}

	public WebsocketSubscriptionProtocol2(String host, String path) {

	}

	@Override
	public void setHandler(ISubscriptionHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void enableSecurity(SEPASecurityManager sm) throws SEPASecurityException {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		// TODO Auto-generated method stub

	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		// TODO Auto-generated method stub

	}
	// protected final Logger logger = LogManager.getLogger();
	//
	// private final SEPAWebsocketClient client;
	//
	// public WebsocketSubscriptionProtocol(String host, int port, String path,
	// SEPASecurityManager sm,
	// ISubscriptionHandler handler) throws SEPAProtocolException,
	// SEPASecurityException {
	// super(handler, sm);
	//
	// try {
	// client = new SEPAWebsocketClient(new URI("wss://" + host + ":" + port +
	// path), handler, sm.getSSLSocket());
	// } catch (URISyntaxException e) {
	// throw new SEPAProtocolException(e);
	// }
	// }
	//
	// public WebsocketSubscriptionProtocol(String host, String path,
	// SEPASecurityManager sm, ISubscriptionHandler handler)
	// throws SEPAProtocolException, SEPASecurityException {
	// super(handler, sm);
	//
	// try {
	// client = new SEPAWebsocketClient(new URI("wss://" + host + path), handler,
	// sm.getSSLSocket());
	// } catch (URISyntaxException e) {
	// throw new SEPAProtocolException(e);
	// }
	// }
	//
	// public WebsocketSubscriptionProtocol(String host, int port, String path,
	// ISubscriptionHandler handler)
	// throws SEPAProtocolException {
	// super(handler, null);
	//
	// try {
	// client = new SEPAWebsocketClient(new URI("ws://" + host + ":" + port + path),
	// handler);
	// } catch (URISyntaxException e) {
	// throw new SEPAProtocolException(e);
	// }
	// }
	//
	// public WebsocketSubscriptionProtocol(String host, String path,
	// ISubscriptionHandler handler)
	// throws SEPAProtocolException {
	// super(handler, null);
	//
	// try {
	// client = new SEPAWebsocketClient(new URI("ws://" + host + path), handler);
	// } catch (URISyntaxException e) {
	// throw new SEPAProtocolException(e);
	// }
	// }
	//
	// @Override
	// public void subscribe(SubscribeRequest request) throws SEPAProtocolException
	// {
	// logger.trace("@subscribe: " + request);
	//
	// while (!client.isOpen()) {
	// logger.debug("connectBlocking...");
	// try {
	// if (client.connectBlocking())
	// logger.debug("Connected");
	// else
	// logger.error("Client is not connected");
	// } catch (InterruptedException e1) {
	// throw new SEPAProtocolException(e1);
	// }
	// }
	//
	// boolean subscribed = false;
	// while (!subscribed) {
	// try {
	// logger.trace("Send");
	// client.send(request.toString());
	// } catch (WebsocketNotConnectedException e) {
	// logger.error("Websocket not connected exception: " + e.getMessage());
	// continue;
	// }
	//
	// subscribed = true;
	// }
	// }
	//
	// @Override
	// public void unsubscribe(UnsubscribeRequest request) throws
	// SEPAProtocolException {
	// logger.debug("@unsubscribe: " + request);
	// try {
	// logger.trace("Send");
	// client.send(request.toString());
	// } catch (WebsocketNotConnectedException e) {
	// logger.warn("Websocket not connected exception: " + e.getMessage());
	// throw new SEPAProtocolException(e);
	// }
	// }
	//
	// @Override
	// public void close() {
	// logger.debug("Close");
	// client.close();
	// }

}
