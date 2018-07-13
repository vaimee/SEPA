package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class SPARQL11SEWebsocket {
	protected Logger logger = LogManager.getLogger();

	private long TIMEOUT = 5000;

	protected SEPAWebsocketClient client = null;
	protected URI wsURI = null;

	public SPARQL11SEWebsocket(String wsUrl) throws SEPAProtocolException {
		try {
			wsURI = new URI(wsUrl);
		} catch (URISyntaxException e) {
			logger.fatal(e.getMessage());
			throw new SEPAProtocolException(e);
		}
	}

	public boolean connect(ISubscriptionHandler handler) throws SEPAProtocolException {
		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Notificaton handler is null"));
		}

		if (client == null) {
			client = new SEPAWebsocketClient(wsURI, handler);
			
			try {
				while(!client.connectBlocking()) {
					Thread.sleep(100);
				}
			} catch (InterruptedException e) {
				throw new SEPAProtocolException(e);
			}
		}
		
		return client.isOpen();
	}

	public Response subscribe(SubscribeRequest req) {
		if (req.getSPARQL() == null) {
			logger.error("SPARQL query is null");
			return new ErrorResponse(-1,500, "SPARQL query is null");
		}

		if (client == null) {
			logger.error("Client is null");
			return new ErrorResponse(-1,500, "Client is null");
		}

		// Send request and wait for response
		return client.sendAndReceive(req.toString(), req.getTimeout());
	}

	public Response unsubscribe(UnsubscribeRequest req) {
		if (req.getSubscribeUUID() == null)
			return new ErrorResponse(-1,500, "SPUID is null");

		if (client == null)
			return new ErrorResponse(-1,500, "Client is null");

		if (client.isClosed())
			return new ErrorResponse(-1,500, "Socket is closed");

		// Send request and wait for response
		return client.sendAndReceive(req.toString(), TIMEOUT);
	}

	public void close() {
		if (client != null)
			client.close();
	}
}
