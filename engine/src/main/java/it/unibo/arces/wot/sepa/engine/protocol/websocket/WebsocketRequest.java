package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import org.java_websocket.WebSocket;

public class WebsocketRequest {
	private WebSocket socket;
	private String message;
	
	public WebsocketRequest(WebSocket socket,String message) {
		this.socket = socket;
		this.message = message;
	}
	
	public WebSocket getSocket() {
		return socket;
	}
	
	public String getMessage() {
		return message;
	}
}
