package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.net.Socket;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class SPARQL11SESecureWebsocket extends SPARQL11SEWebsocket {	
	private Socket secureSocket = null;
	
	public SPARQL11SESecureWebsocket(String wsUrl,SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(wsUrl);

		try {
			secureSocket = sm.getSSLSocket();
		} catch (IOException e) {
			throw new SEPASecurityException(e);
		}
	}

	public boolean connect(ISubscriptionHandler handler) throws SEPAProtocolException {
		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new SEPAProtocolException(new IllegalArgumentException("Notificaton handler is null"));
		}

		if (client == null) {
			client = new SEPAWebsocketClient(wsURI, handler);
			
			// Enable secure socket
			client.setSocket(secureSocket);
						
			try {
				return client.connectBlocking();
			} catch (InterruptedException e) {
				throw new SEPAProtocolException(e);
			}
		}
		
		return client.isOpen();
	}
}
