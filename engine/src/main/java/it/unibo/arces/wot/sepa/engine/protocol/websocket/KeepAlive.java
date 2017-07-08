package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.nio.channels.NotYetConnectedException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

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

	private ConcurrentHashMap<WebSocket, ResponseAndNotificationListener> activeSockets;
	private ConcurrentHashMap<WebSocket, HashSet<String>> activeSubscriptions;
	private Scheduler scheduler;

	private final Logger logger = LogManager.getLogger("KeepAlive");

	public KeepAlive(long timeout, ConcurrentHashMap<WebSocket, ResponseAndNotificationListener> activeSockets,
			ConcurrentHashMap<WebSocket, HashSet<String>> activeSubscriptions, Scheduler scheduler) {
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
			KeySetView<WebSocket, ResponseAndNotificationListener> activeSocketToBeChecked;

			synchronized (activeSockets) {
				activeSocketToBeChecked = activeSockets.keySet();
			}

			for (WebSocket socket : activeSocketToBeChecked) {

				// Send ping only on sockets with active subscriptions
				synchronized (activeSubscriptions) {
					if (activeSubscriptions.get(socket).size() == 0)
						continue;
				}

				try {
					Ping ping = new Ping();
					socket.send(ping.toString());
				} catch (WebsocketNotConnectedException | NotYetConnectedException e) {
					synchronized(activeSubscriptions) {
						unsubscribeAll(activeSubscriptions.get(socket));
						activeSubscriptions.remove(socket);
					}
					synchronized (activeSockets) {
						activeSockets.remove(socket);
					}
				}
				
				
			}
		}
	}

	private void unsubscribeAll(HashSet<String> subIDSet) {
		logger.debug("@unsubscribeAll");

		// synchronized (activeSubscriptions) {
		// HashSet<String> subIDSet = activeSubscriptions.get(socket);
		// }

		for (String subId : subIDSet) {
			int token = scheduler.getToken();
			if (token == -1) {
				logger.error("No more tokens");
				continue;
			}
			logger.debug(">> Scheduling UNSUBSCRIBE request #" + token);

			scheduler.addRequest(new UnsubscribeRequest(token, subId), null);
		}

	}
}
