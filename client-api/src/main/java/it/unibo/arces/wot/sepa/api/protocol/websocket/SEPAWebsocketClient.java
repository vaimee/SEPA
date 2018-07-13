package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URI;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class SEPAWebsocketClient extends WebSocketClient {
	protected final Logger logger = LogManager.getLogger();

	private ISubscriptionHandler handler;
	private Response response = new ErrorResponse(500, "null");
	private Semaphore mutex = new Semaphore(0);
	private boolean responseReceived = false;

	public SEPAWebsocketClient(URI wsUrl, ISubscriptionHandler handler) {
		super(wsUrl);

		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new IllegalArgumentException("Notificaton handler is null");
		}

		this.handler = handler;
	}

	private Response waitResponse(long timeout) {
		synchronized (mutex) {
			while (!responseReceived) {
				try {
					mutex.wait(timeout);
				} catch (InterruptedException e) {
					return new ErrorResponse(-1,500, "Interrupted exception");
				}
			}
		}

		return response;
	}

	private void setResponse(Response response) {
		synchronized (mutex) {
			this.response = response;
			responseReceived = true;
			mutex.notify();
		}
	}

	public Response sendAndReceive(String message, long timeout) {
		// Send request and wait response
		responseReceived = false;

		try {
			if (isOpen()) {
				send(message);
				return waitResponse(timeout);
			}
		} catch (WebsocketNotConnectedException e) {
			return new ErrorResponse(-1,500, "WebsocketNotConnectedException");
		}

		return new ErrorResponse(-1,500, "Socket is closed");
		
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		logger.debug("@onOpen STATE: " + getReadyState());
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		logger.debug(
				"@onClose code: " + code + " reason: " + reason + " remote: " + remote + " STATE: " + getReadyState());
		if (handler != null)
			handler.onBrokenConnection();
	}

	@Override
	public void onError(Exception ex) {
		ErrorResponse error = new ErrorResponse(-1,500, "onError: "+ex.getMessage());

		logger.debug("@onError: " + error + " STATE: " + getReadyState());

		if (handler != null)
			handler.onError(error);
	}

	@Override
	public void onMessage(String message) {
		logger.debug("@onMessage " + message);

		// Parse message
		JsonObject jsonMessage = null;
		try {
			jsonMessage = new JsonParser().parse(message).getAsJsonObject();
		} catch (IllegalStateException e) {
			logger.error(e.getMessage());
			return;
		}

		if (jsonMessage.has("notification")) {
			JsonObject notification = jsonMessage.get("notification").getAsJsonObject();
			if (notification.get("sequence").getAsInt() == 0) {
				setResponse(new SubscribeResponse(jsonMessage));
			} else if (handler != null)
				handler.onSemanticEvent(new Notification(jsonMessage));
		} else if (jsonMessage.has("error")) {
			setResponse(new ErrorResponse(jsonMessage));
			if (handler != null)
				handler.onError((ErrorResponse) response);
		} else if (jsonMessage.has("unsubscribed")) {
			setResponse(new UnsubscribeResponse(jsonMessage));
		} else
			logger.error("Unknown message: " + message);
	}
}
