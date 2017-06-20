package it.unibo.arces.wot.sepa.engine.protocol.websocket;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.WebSocket;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class ResponseListener implements ResponseAndNotificationListener {
	protected Logger logger = LogManager.getLogger("WebsocketListener");
	
	private WebSocket socket;
	private HashSet<String> spuIds;
	private Scheduler scheduler;
	
	public ResponseListener(WebSocket socket,Scheduler scheduler,HashSet<String> spuIds) throws IllegalArgumentException {
		this.socket = socket;
		this.scheduler = scheduler;
		this.spuIds = spuIds;
		
		if (socket == null || scheduler == null || spuIds == null) throw new IllegalArgumentException("One or more arguments are null");
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
}
