package it.unibo.arces.wot.sepa;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TyrusWebsocketClient extends Endpoint {
	protected Logger logger = LogManager.getLogger();
	
	protected final Sync sync;
	
	public TyrusWebsocketClient(Sync s) {
		sync = s;
	}
	
	@Override
	public void onOpen(Session session, EndpointConfig arg1) {
		logger.info("onOpen session: "+session.getId());
		
		sync.event();

	}

}
