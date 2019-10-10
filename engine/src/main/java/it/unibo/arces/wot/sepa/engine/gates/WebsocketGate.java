package it.unibo.arces.wot.sepa.engine.gates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketGate extends Gate {
	protected static final Logger logger = LogManager.getLogger();
	
	protected final WebSocket socket;
	
	public WebsocketGate(WebSocket s,Scheduler scheduler){
		super(scheduler);
		this.socket = s;
	}
	
	public void send(String ret) throws SEPAProtocolException {
		try{
			socket.send(ret);
			logger.debug("Sent: "+ret);
		}
		catch(Exception e){
			logger.warn("Socket: "+socket.hashCode()+" failed to send response: "+ret+" Exception:"+e.getMessage());
			throw new SEPAProtocolException(e);
		}	
	}

	@Override
	public boolean ping() {
		return socket.isOpen();
	}
}
