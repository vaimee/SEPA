package it.unibo.arces.wot.sepa.api.protocol.websocket;

import org.apache.http.HttpStatus;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.ISubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
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

	public WebSocketSubscriptionProtocol(String host, int port, String path, SEPASecurityManager sm) throws SEPAProtocolException, SEPASecurityException {
		// WSS
		if (port != -1)
			wssClient = new SPARQL11SESecureWebsocket("wss://" + host + ":" + port + path, sm);
		else
			wssClient = new SPARQL11SESecureWebsocket("wss://" + host + path,sm);

	}

	@Override
	public void setHandler(ISubscriptionHandler handler) throws SEPAProtocolException {
		if (wsClient != null)
			wsClient.setHandler(handler);
		else if (wssClient != null)
			wssClient.setHandler(handler);
	}

	@Override
	public void close() {
		if (wsClient != null)
			wsClient.close();
		if (wssClient != null)
			wssClient.close();
	}

	@Override
	public Response subscribe(SubscribeRequest request) {
		if (request.getAuthorizationHeader() == null) {
			if (wsClient == null)
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Client not initialized");
			return wsClient.subscribe(request);
		} else {
			if (wssClient == null)
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Client not initialized");

			return wssClient.subscribe(request);
		}
	}

	@Override
	public Response unsubscribe(UnsubscribeRequest request) {
		if (request.getAuthorizationHeader() == null) {
			if (wsClient == null)
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Client not initialized");
			return wsClient.unsubscribe(request);
		} else {
			if (wssClient == null)
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Client not initialized");

			return wssClient.unsubscribe(request);
		}
	}

	@Override
	public boolean isSecure() {
		return wssClient != null;
	}
}
