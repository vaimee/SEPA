package it.unibo.arces.wot.sepa.api.protocols.websocket;

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

public class SEPAWebsocketClient extends WebSocketClient {
	protected final Logger logger = LogManager.getLogger();

	private final ISubscriptionHandler handler;
	
	public SEPAWebsocketClient(URI wsUrl, ISubscriptionHandler handler, Socket secure) {
		super(wsUrl);

		this.handler = handler;

		setSocket(secure);
	}

	public SEPAWebsocketClient(URI wsUrl, ISubscriptionHandler handler) {
		super(wsUrl);

		this.handler = handler;
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		logger.debug("@onOpen: "+handshakedata.getHttpStatusMessage());
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		logger.debug("@onClose code:" + code + " reason:" + reason + " remote:" + remote);
		
		try {
			handler.onBrokenConnection();
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
	}

	@Override
	public void onError(Exception ex) {
		ErrorResponse error = new ErrorResponse(500, "Exception", ex.getMessage());

		logger.debug("@onError: " + error);

		try {
			handler.onError(error);
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
	}

	@Override
	public void onMessage(String message) {
		logger.debug("@onMessage: " + message);

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
				handler.onError(new ErrorResponse(jsonMessage.get("status_code").getAsInt(),jsonMessage.get("error").getAsString(),jsonMessage.get("error_description").getAsString()));
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
