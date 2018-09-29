package it.unibo.arces.wot.sepa.api.protocols.websocket;

import javax.net.ssl.SSLContext;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class TyrusWebsocketClient extends Endpoint {
	protected Logger logger = LogManager.getLogger();

	protected final ISubscriptionHandler handler;
	protected final ClientManager client;

	public TyrusWebsocketClient(ISubscriptionHandler handler) {
		this.handler = handler;
		
		client = ClientManager.createClient();
	}

	public TyrusWebsocketClient(ISubscriptionHandler handler, SSLContext sslContext) {
		this.handler = handler;
		
		client = ClientManager.createClient();
		SslEngineConfigurator config = new SslEngineConfigurator(sslContext);
		config.setHostVerificationEnabled(false);
		client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, config);
	}

	@Override
	public void onOpen(Session session, EndpointConfig arg1) {
		logger.info("onOpen session: " + session.getId());

		session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				logger.debug("@onMessage: " + message);

				// Parse message
				JsonObject jsonMessage = null;
				try {
					jsonMessage = new JsonParser().parse(message).getAsJsonObject();
				} catch (Exception e) {
					logger.error(e.getMessage());
					return;
				}

				if (jsonMessage.has("notification")) {
					JsonObject notification = jsonMessage.get("notification").getAsJsonObject();

					// Subscribe
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

					// Event
					try {
						handler.onSemanticEvent(new Notification(jsonMessage));
					} catch (Exception e) {
						logger.error("Handler is null " + e.getMessage());
					}
				} else if (jsonMessage.has("error")) {
					try {
						handler.onError(new ErrorResponse(jsonMessage.get("status_code").getAsInt(),
								jsonMessage.get("error").getAsString(), jsonMessage.get("error_description").getAsString()));
					} catch (Exception e) {
						logger.error("Handler is null " + e.getMessage());
					}
				} else if (jsonMessage.has("unsubscribed")) {
					try {
						handler.onUnsubscribe(jsonMessage.get("unsubscribed").getAsJsonObject().get("spuid").getAsString());
					} catch (Exception e) {
						logger.error("Handler is null " + e.getMessage());
					}
				} else
					logger.error("Unknown message: " + message);
			}
		});
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		try {
			handler.onBrokenConnection();
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
	}

	@Override
	public void onError(Session session, Throwable thr) {
		ErrorResponse error = new ErrorResponse(500, "Exception", thr.getMessage());

		logger.debug("@onError: " + error);

		try {
			handler.onError(error);
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
		try {
			handler.onError(error);
		} catch (Exception e) {
			logger.error("Handler is null " + e.getMessage());
		}
	}
}
