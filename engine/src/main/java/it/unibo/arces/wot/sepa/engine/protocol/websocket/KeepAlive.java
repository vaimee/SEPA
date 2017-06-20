package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class KeepAlive extends Thread {
	private long timeout;
	
	private HashMap<WebSocket, ResponseAndNotificationListener> activeSockets;
	private HashMap<WebSocket, HashSet<String>>  activeSubscriptions;
	private Scheduler scheduler;
	
	private final Logger logger = LogManager.getLogger("KeepAlive");	
	
	public KeepAlive(long timeout,HashMap<WebSocket, ResponseAndNotificationListener> activeSockets,HashMap<WebSocket, HashSet<String>> activeSubscriptions,Scheduler scheduler){
		this.timeout = timeout;
		this.activeSockets = activeSockets;
		this.activeSubscriptions = activeSubscriptions;
		this.scheduler = scheduler;
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
			ArrayList<WebSocket> brokenSockets = new ArrayList<WebSocket>();
			
			synchronized (activeSockets) {
				for (WebSocket socket : activeSockets.keySet()) {
					// Send ping only on sockets with active subscriptions
					if (activeSubscriptions.get(socket).size() == 0)
						continue;

					try {
						Ping ping = new Ping();
						socket.send(ping.toString());	
					}
					catch(WebsocketNotConnectedException | NotYetConnectedException e) {
						logger.debug("Broken socket: remove all active subscriptions");
						unsubscribeAll(socket);
						brokenSockets.add(socket);
					}
				}
				
				for (WebSocket broken : brokenSockets) {
					activeSockets.remove(broken);
					activeSubscriptions.remove(broken);
				}
			}
		}
	}
	
	private void unsubscribeAll(WebSocket socket) {
		logger.debug("@unsubscribeAll");

		Iterator<String> it = activeSubscriptions.get(socket).iterator();

		while (it.hasNext()) {
			int token = scheduler.getToken();
			if (token == -1) {
				logger.error("No more tokens");
				continue;
			}
			logger.debug(">> Scheduling UNSUBSCRIBE request #" + token);
			scheduler.addRequest(new UnsubscribeRequest(token, it.next()), activeSockets.get(socket));
		}
	}
}
