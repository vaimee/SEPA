package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.io.IOException;
import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.WebsocketBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class WebsocketEventHandler implements EventHandler {
	private final Logger logger = LogManager.getLogger("WebsocketEventHandler");
	
	private WebSocket socket;
	private WebsocketBeans jmx;
	private Instant start;
	
	public WebsocketEventHandler(WebSocket s,WebsocketBeans jmx){
		socket = s;
		this.jmx = jmx;
		start = Instant.now();
	}
	
	private void send(Response ret) throws IOException {
		if (!socket.isOpen()) throw new IOException("Socket closed");
		try{
			socket.send(ret.toString());
		}
		catch(WebsocketNotConnectedException e){
			throw new IOException(e.getMessage());
		}	
	}
	
	@Override
	public void sendResponse(Response response) throws IOException {
		long timing = 0;
		if (response.isSubscribeResponse())
			timing = jmx.subscribeTimings(start);
		else if (response.isUnsubscribeResponse())
			timing = jmx.unsubscribeTimings(start);
			
		logger.info("Response #"+response.getToken()+" ("+timing+" ms)");
		logger.debug(response);
		send(response);
	}

	@Override
	public void notifyEvent(Notification notify) throws IOException {
		send(notify);
	}

	@Override
	public void sendPing(Ping ping) throws IOException {
		send(ping);
	}

}
