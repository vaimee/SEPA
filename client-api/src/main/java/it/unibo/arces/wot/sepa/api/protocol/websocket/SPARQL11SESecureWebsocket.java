package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.io.IOException;
import java.net.Socket;

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

	@Override
	protected boolean connect() {
		if (client == null)
			try {
				client = new SEPAWebsocketClient(wsURI, handler);
				// Enable secure socket
				client.setSocket(secureSocket);
				
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
}
