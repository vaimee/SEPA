package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.WebSocket;

import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.RequestResponseHandler.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class WebsocketListener implements ResponseAndNotificationListener {
	private WebSocket socket;
	
	private HashSet<String> spuIds = new HashSet<String>();
	protected Logger logger = LogManager.getLogger("WebsocketListener");
	private Scheduler scheduler;
	
	private HashMap<WebSocket, WebsocketListener> activeSockets;
	
	public WebsocketListener(WebSocket socket,Scheduler scheduler,HashMap<WebSocket, WebsocketListener> activeSockets) {
		this.socket = socket;
		this.scheduler = scheduler;
		this.activeSockets = activeSockets;
	}
	
	public int activeSubscriptions() {
		return spuIds.size();
	}

	public void unsubscribeAll() {
		synchronized (spuIds) {
			Iterator<String> it = spuIds.iterator();

			while (it.hasNext()) {
				int token = scheduler.getToken();
				if (token == -1) {
					logger.error("No more tokens");
					continue;
				}
				logger.debug(">> Scheduling UNSUBSCRIBE request #" + token);
				scheduler.addRequest(new UnsubscribeRequest(token, it.next()), this);
			}
		}
	}

	@Override
	public void notify(Response response) {
		if (response.getClass().equals(SubscribeResponse.class)) {
			logger.debug("<< SUBSCRIBE response #" + response.getToken());

			synchronized (spuIds) {
				spuIds.add(((SubscribeResponse) response).getSpuid());
			}

		} else if (response.getClass().equals(UnsubscribeResponse.class)) {
			logger.debug("<< UNSUBSCRIBE response #" + response.getToken() + " ");

			synchronized (spuIds) {
				spuIds.remove(((UnsubscribeResponse) response).getSpuid());

				synchronized (activeSockets) {
					if (spuIds.isEmpty())
						activeSockets.remove(socket);
				}
			}
		}

		// Send response to client
		if (socket != null)
			if (socket.isOpen())
				socket.send(response.toString());

		// Release token
		if (!response.getClass().equals(Notification.class))
			scheduler.releaseToken(response.getToken());
	}

	public Set<String> getSPUIDs() {
		return spuIds;
	}
}
