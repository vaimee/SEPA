package it.unibo.arces.wot.sepa.engine.gates.websocket;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import org.apache.http.HttpStatus;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.gates.SecureWebsocketGate;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SecureWebsocketServer extends WebsocketServer {

	@Override
	protected String getWelcomeMessage() {
		return "SPARQL 1.1 Subscribe | wss://%s:%d%s";
	}

	public SecureWebsocketServer(int port, String path, Scheduler scheduler)
			throws SEPAProtocolException, SEPASecurityException {
		super(port, path, scheduler);

		setWebSocketFactory(new DefaultSSLWebSocketServerFactory(Dependability.getSSLContext()));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		logger.trace("@onOpen: " + conn + " Resource descriptor: " + conn.getResourceDescriptor());

		if (!conn.getResourceDescriptor().equals(path)) {
			logger.warn("@onOpen Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_NOT_FOUND, "wrong_path",
					"Bad resource descriptor: " + conn.getResourceDescriptor() + " Use: " + path);
			conn.send(response.toString());
			return;
		}

		// Add new gate
		synchronized (gates) {
			SecureWebsocketGate handler = new SecureWebsocketGate(conn, scheduler);
			gates.put(conn, handler);

			fragmentedMessages.put(conn, null);

			logger.debug("@onOpen socket: " + conn.hashCode() + " UUID: " + handler.getGID() + " Total sockets: "
					+ gates.size());
		}
	}
}
