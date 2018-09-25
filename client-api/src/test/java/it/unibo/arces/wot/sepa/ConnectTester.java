package it.unibo.arces.wot.sepa;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class ConnectTester extends WebSocketClient {
	protected Logger logger = LogManager.getLogger();
	protected String url = null;
	protected final Sync sync;
	
	public ConnectTester(URI server,Sync s,SEPASecurityManager sm) throws URISyntaxException, SEPASecurityException {
		super(server);
		sync = s;
		
		if (sm != null) setSocket(sm.getSSLSocket());
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		logger.debug("@onOpen: " + handshakedata.getHttpStatusMessage());
		
		sync.event();
	}

	@Override
	public void onMessage(String message) {
		logger.debug("@onMessage: " + message);
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		logger.warn("@onClose code:" + code 
				+ " reason:" + reason 
				+ " remote:" + remote
				+ " STATE: "+this.getReadyState());
//				+" isConnecting: "+isConnecting()
//				+" isClosed: "+this.isClosed()
//				+" isClosing: "+this.isClosing()
//				+" isOpen:" + this.isOpen()
//				+" isFulshAndClose: "+this.isFlushAndClose()
//				+" isReuseAddr: "+this.isReuseAddr());
		
		if (remote && code == -1 && getReadyState().equals(READYSTATE.NOT_YET_CONNECTED)) close();
	}

	@Override
	public void onError(Exception ex) {
		logger.error("@onError "+ex.getMessage());	
		
		//reconnect();
	}

}
