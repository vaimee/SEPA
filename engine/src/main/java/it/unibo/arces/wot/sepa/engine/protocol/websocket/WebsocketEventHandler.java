package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.io.IOException;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class WebsocketEventHandler implements EventHandler {
	
	private WebSocket socket;
	
	public WebsocketEventHandler(WebSocket s){
		socket = s;
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
