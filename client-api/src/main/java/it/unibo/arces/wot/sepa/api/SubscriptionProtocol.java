package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;
//import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public interface SubscriptionProtocol extends Closeable {
	static final Logger logger = LogManager.getLogger();
	
	public void setHandler(ISubscriptionHandler handler);
	
	public void enableSecurity(SEPASecurityManager sm) throws SEPASecurityException;

//	public void close() throws IOException;

	public void subscribe(SubscribeRequest request) throws SEPAProtocolException;

	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException;
	
	public static void onMessage(String message, ISubscriptionHandler handler) {
		logger.trace("@onMessage: " + message);

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
					logger.trace("Subscribed: " + spuid + " alias: " + alias);
					handler.onSubscribe(spuid, alias);
				} catch (Exception e) {
					logger.error("Handler is null " + e.getMessage());
					return;
				}
			}

			// Event
			try {
				Notification notify = new Notification(jsonMessage);
				logger.trace("Notification: " + notify);
				handler.onSemanticEvent(notify);
			} catch (Exception e) {
				logger.error("Handler is null " + e.getMessage());
			}
		} else if (jsonMessage.has("error")) {
			ErrorResponse error = new ErrorResponse(jsonMessage.get("status_code").getAsInt(),
					jsonMessage.get("error").getAsString(), jsonMessage.get("error_description").getAsString());
			logger.error(error);
			try {
				handler.onError(error);
			} catch (Exception e) {
				logger.error("Handler is null " + e.getMessage());
			}
		} else if (jsonMessage.has("unsubscribed")) {
			logger.debug("unsubscribed");
			try {
				handler.onUnsubscribe(
						jsonMessage.get("unsubscribed").getAsJsonObject().get("spuid").getAsString());
			} catch (Exception e) {
				logger.error("Handler is null " + e.getMessage());
			}
		} else
			logger.error("Unknown message: " + message);
	}
	
}
