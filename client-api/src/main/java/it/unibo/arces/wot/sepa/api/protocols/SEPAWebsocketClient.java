package it.unibo.arces.wot.sepa.api.protocols;

import java.net.Socket;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

class SEPAWebsocketClient extends WebSocketClient {
	protected final Logger logger = LogManager.getLogger();

	private ISubscriptionHandler handler;

	public SEPAWebsocketClient(URI wsUrl, ISubscriptionHandler handler, Socket secure) {
		super(wsUrl);

		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new IllegalArgumentException("Notificaton handler is null");
		}

		this.handler = handler;

		setSocket(secure);
	}

	public SEPAWebsocketClient(URI wsUrl, ISubscriptionHandler handler) {
		super(wsUrl);

		if (handler == null) {
			logger.fatal("Notification handler is null. Client cannot be initialized");
			throw new IllegalArgumentException("Notificaton handler is null");
		}

		this.handler = handler;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		logger.debug("@onOpen STATE: " + getReadyState());
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		logger.debug(
				"@onClose code:" + code + " reason:" + reason + " remote:" + remote + " state:" + getReadyState());
		if (handler != null)
			handler.onBrokenConnection();
	}

	@Override
	public void onError(Exception ex) {
		ErrorResponse error = new ErrorResponse(500, "onError: " + ex.getMessage());

		logger.debug("@onError: " + error + " STATE: " + getReadyState());

		try {
			handler.onError(error);
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
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
				String spuid = notification.get("spuid").getAsString();
				String alias = null;
				if (notification.has("alias"))
					alias = notification.get("alias").getAsString();
				try {
					handler.onSubscribe(spuid, alias);
				} catch (Exception e) {
					logger.error("Handler is null " + e.getMessage());
					return;
				}
			}
			try {
				handler.onSemanticEvent(new Notification(jsonMessage));
			} catch (Exception e) {
				logger.error("Handler is null " + e.getMessage());
			}
		} else if (jsonMessage.has("error")) {
			try{
				handler.onError(new ErrorResponse(jsonMessage));
			}
			catch(Exception e) {
				logger.error("Handler is null "+e.getMessage());
			}
		} else if (jsonMessage.has("unsubscribed")) {
			try {
				handler.onUnsubscribe(jsonMessage.get("unsubscribed").getAsJsonObject().get("spuid").getAsString());
			}
			catch(Exception e) {
				logger.error("Handler is null "+e.getMessage());
			}
		} else
			logger.error("Unknown message: " + message);
	}
}
