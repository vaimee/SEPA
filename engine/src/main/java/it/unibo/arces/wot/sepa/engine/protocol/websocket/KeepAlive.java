package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.util.HashMap;

import org.java_websocket.WebSocket;

//import org.glassfish.grizzly.websockets.WebSocket;

import it.unibo.arces.wot.sepa.commons.response.Ping;

public class KeepAlive extends Thread {
	private long timeout;
	private HashMap<WebSocket, WebsocketListener> activeSockets;

	public KeepAlive(long timeout,HashMap<WebSocket, WebsocketListener> activeSockets){
		this.timeout = timeout;
		this.activeSockets = activeSockets;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				return;
			}

			// Send heart beat on each active socket to detect broken
			// sockets
			synchronized (activeSockets) {
				for (WebSocket socket : activeSockets.keySet()) {
					// Send ping only on sockets with active subscriptions
					if (activeSockets.get(socket).activeSubscriptions() == 0)
						continue;

					if (socket.isOpen()) {
						Ping ping = new Ping();
						socket.send(ping.toString());
					} else
						activeSockets.get(socket).unsubscribeAll();

				}
			}
		}
	}
}
