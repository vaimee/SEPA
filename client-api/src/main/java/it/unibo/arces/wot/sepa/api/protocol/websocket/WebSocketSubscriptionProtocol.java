package it.unibo.arces.wot.sepa.api.protocol.websocket;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.ISubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebSocketSubscriptionProtocol implements ISubscriptionProtocol {
	private SPARQL11SEWebsocket wsClient = null;
	private SPARQL11SESecureWebsocket wssClient = null;

	public WebSocketSubscriptionProtocol(String host, int port, String path) throws SEPAProtocolException {
		// WS
		if (port != -1)
			wsClient = new SPARQL11SEWebsocket("ws://" + host + ":" + port + path);
		else
			wsClient = new SPARQL11SEWebsocket("ws://" + host + path);
	}

	public WebSocketSubscriptionProtocol(String host, int port, String path, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		// WSS
		if (port != -1)
			wssClient = new SPARQL11SESecureWebsocket("wss://" + host + ":" + port + path, sm);
		else
			wssClient = new SPARQL11SESecureWebsocket("wss://" + host + path, sm);

	}

	@Override
	public boolean connect(ISubscriptionHandler handler) throws SEPAProtocolException {
		if (!isSecure()) return wsClient.connect(handler);
		else return wssClient.connect(handler);
	}

	@Override
	public void close() {
		if (!isSecure())
			wsClient.close();
		else
			wssClient.close();
	}

	@Override
	public Response subscribe(SubscribeRequest request) {
		if (!isSecure()) return wsClient.subscribe(request);
			
		return wssClient.subscribe(request);	
	}

	@Override
	public Response unsubscribe(UnsubscribeRequest request) {
		if (!isSecure())
			return wsClient.unsubscribe(request);
		else
			return wssClient.unsubscribe(request);
	}

	@Override
	public boolean isSecure() {
		return wssClient != null;
	}
}
