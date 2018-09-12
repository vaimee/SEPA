package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

public class WebsocketEventHandler extends ResponseHandler implements EventHandler {
	private static final Logger logger = LogManager.getLogger();
	
	private final WebSocket socket;
	
	public WebsocketEventHandler(WebSocket s){
		this.socket = s;
	}
	
	private void send(Response ret) throws SEPAProtocolException {
		try{
			socket.send(ret.toString());
		}
		catch(WebsocketNotConnectedException e){
			logger.error("Socket: "+socket.hashCode()+" failed to send response: "+ret+" Exception:"+e.getMessage());
			throw new SEPAProtocolException(e.getMessage());
		}	
	}
	
	@Override
	public void sendResponse(Response response) throws SEPAProtocolException {		
		logger.trace(response);

		
		send(response);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		WebsocketBeans.notification();
		
		send(notify);
	}
}
