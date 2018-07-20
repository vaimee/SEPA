package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityManager;

public class WebsocketEventHandler implements EventHandler ,ResponseHandler {
	private static final Logger logger = LogManager.getLogger();
	
	private final WebSocket socket;
	
	// Dependability manager
	private final DependabilityManager dependabilityMng;
	
	public WebsocketEventHandler(WebSocket s,DependabilityManager dependabilityMng){
		this.socket = s;
		this.dependabilityMng = dependabilityMng;
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
		
		if (response.isSubscribeResponse()) {
			dependabilityMng.onSubscribe(socket.hashCode(), ((SubscribeResponse)response).getSpuid());
		}
		else if (response.isUnsubscribeResponse()) {
			dependabilityMng.onUnsubscribe(socket.hashCode(), ((UnsubscribeResponse)response).getSpuid());
		}
		else if (response.isError()) {
			logger.error(response);
			dependabilityMng.onError(socket.hashCode(), (ErrorResponse)response);
		}
		
		send(response);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		send(notify);
	}
}
