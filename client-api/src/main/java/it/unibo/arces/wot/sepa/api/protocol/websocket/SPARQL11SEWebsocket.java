package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class SPARQL11SEWebsocket {
	protected Logger logger = LogManager.getLogger();

	private long TIMEOUT = 5000;

	protected SEPAWebsocketClient client = null;
	protected URI wsURI = null;
	protected ISubscriptionHandler handler = null;

	public SPARQL11SEWebsocket(String wsUrl) throws SEPAProtocolException {
		try {	
			wsURI = new URI(wsUrl);
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}
	
	public void setHandler(ISubscriptionHandler handler) throws SEPAProtocolException {
		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Notificaton handler is null"));
		}	
		
		this.handler = handler;
	}

	public Response subscribe(String sparql, String alias) {
		return _subscribe(sparql, null, alias);
	}

	public Response subscribe(String sparql) {
		return _subscribe(sparql, null, null);
	}

	protected boolean connect() throws SEPAProtocolException {
		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Notificaton handler is null"));
		}	
		
		if (client == null)
			try {
				client = new SEPAWebsocketClient(wsURI, handler);
				if (!client.connectBlocking()) {
					logger.error("Not connected");
					return false;
				}
			} catch (InterruptedException e) {
				logger.debug(e);
				return false;
			}
		return true;
	}

	protected Response _subscribe(String sparql, String authorization, String alias) {
		if (sparql == null)
			return new ErrorResponse(500, "SPARQL query is null");

		try {
			if (!connect()) {
				return new ErrorResponse(500, "Failed to connect");
			}
		} catch (SEPAProtocolException e1) {
			return new ErrorResponse(500,e1.getMessage());
		}
		
		if (!client.isOpen())
			try {
				client.reconnectBlocking();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return new ErrorResponse(500, "Failed to re-connect");
			}
		
		// Create SPARQL 1.1 Subscribe request
		JsonObject body = new JsonObject();
		JsonObject request = new JsonObject();
		body.add("sparql", new JsonPrimitive(sparql));
		if (authorization != null)
			body.add("authorization", new JsonPrimitive(authorization));
		if (alias != null)
			body.add("alias", new JsonPrimitive(alias));
		request.add("subscribe", body);

		// Send request and wait for response
		return client.sendAndReceive(request.toString(), TIMEOUT);
	}

	public Response unsubscribe(String spuid) {
		return _unsubscribe(spuid, null);
	}

	protected Response _unsubscribe(String spuid, String authorization) {
		if (spuid == null)
			return new ErrorResponse(500, "SPUID is null");
		
		if (client == null)
			return new ErrorResponse(500, "Client not connected");
		
		if (client.isClosed())
			return new ErrorResponse(500, "Socket is closed");
		
		// Create SPARQL 1.1 Unsubscribe request
		JsonObject request = new JsonObject();
		JsonObject body = new JsonObject();
		body.add("spuid", new JsonPrimitive(spuid));
		if (authorization != null)
			body.add("authorization", new JsonPrimitive(authorization));
		request.add("unsubscribe", body);

		// Send request and wait for response
		return client.sendAndReceive(request.toString(), TIMEOUT);
	}

	public void close() {
		if (isConnected()) {
			client.close();
		}
	}

	private boolean isConnected() {
		return client != null && client.isOpen();
	}

}
