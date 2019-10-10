package it.unibo.arces.wot.sepa.engine.gates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import org.java_websocket.WebSocket;


public class SecureWebsocketGate extends WebsocketGate {
	protected static final Logger logger = LogManager.getLogger();
	
	
	public SecureWebsocketGate(WebSocket s,Scheduler scheduler){
		super(s,scheduler);
		enableAuthorization();
	}
}
