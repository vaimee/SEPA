package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.io.IOException;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.response.Notification;
//import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.dependability.DependabilityManager;

public class WebsocketEventHandler implements EventHandler {
	private static final Logger logger = LogManager.getLogger();
	
	private WebSocket socket;
	private WebsocketBeans jmx;
	private Instant start;
	
	// Dependability manager
	private DependabilityManager dependabilityMng;
	
	public WebsocketEventHandler(WebSocket s,WebsocketBeans jmx,DependabilityManager dependabilityMng){
		socket = s;
		this.jmx = jmx;
		this.dependabilityMng = dependabilityMng;
	}
	
	private void send(Response ret) throws IOException {
		try{
			socket.send(ret.toString());
		}
		catch(WebsocketNotConnectedException e){
			throw new IOException(e.getMessage());
		}	
	}
	
	public void startTiming() {
		start = Instant.now();
	}
	
	@Override
	public void sendResponse(Response response) throws IOException {
		long timing = 0;
		if (response.isSubscribeResponse()) {
			timing = jmx.subscribeTimings(start);
			dependabilityMng.onSubscribe(socket, ((SubscribeResponse)response).getSpuid());
		}
		else if (response.isUnsubscribeResponse()) {
			timing = jmx.unsubscribeTimings(start);
			dependabilityMng.onUnsubscribe(socket, ((UnsubscribeResponse)response).getSpuid());
		}
			
		logger.debug("Response #"+response.getToken()+" ("+timing+" ms)");
		logger.trace(response);
		send(response);
	}

	@Override
	public void notifyEvent(Notification notify) throws IOException {
		send(notify);
	}
}
