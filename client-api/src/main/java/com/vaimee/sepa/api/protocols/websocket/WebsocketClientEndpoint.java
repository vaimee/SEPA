/* The client side Websokets protocol implementation 
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.api.protocols.websocket;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.api.commons.security.ClientSecurityManager;
import com.vaimee.sepa.logging.Logging;


public class WebsocketClientEndpoint extends Endpoint implements Closeable {
	protected final ISubscriptionHandler handler;

	protected final ClientManager client;
	protected Session session;

	public WebsocketClientEndpoint(ClientSecurityManager sm,ISubscriptionHandler handler) throws SEPASecurityException {
		client = ClientManager.createClient();
		client.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 200*1024*1024);
		
		if (sm != null) {
			SslEngineConfigurator config = new SslEngineConfigurator(sm.getSSLContext());
			config.setHostVerificationEnabled(false);
			client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, config);
		}
		
		this.handler = handler;
	}
	
	public void connect(URI url) throws SEPAProtocolException {
		try {
			session = client.connectToServer(this, url);
		} catch (DeploymentException | IOException e) {
			Logging.logger.error("Connect to: "+url+" exception "+e.getClass().getName()+" "+e.getMessage());
			throw new SEPAProtocolException(e.getMessage());
		}
	}

	@Override
	public void close() throws IOException {
		Logging.logger.trace("Close");
		
		if (session != null)
			session.close();
		
		client.shutdown();
	}

	public void onOpen(Session session, EndpointConfig config) {
		Logging.logger.trace("@onOpen session: " + session.getId());

		this.session = session;

		session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				Logging.logger.trace("@onMessage: " + message);

				// Parse message
				JsonObject jsonMessage = null;
				try {
					jsonMessage = new Gson().fromJson(message, JsonObject.class);
				} catch (Exception e) {
					Logging.logger.error("Exception on parsing message: " + message + " exception: " + e.getMessage());
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
							Logging.logger.trace("Subscribed: " + spuid + " alias: " + alias);
							handler.onSubscribe(spuid, alias);
						} catch (Exception e) {
							e.printStackTrace();
							Logging.logger.error("Exception on handling onSubscribe. Handler: " + handler + " Exception: "
									+ e.getMessage());
							return;
						}
					}

					// Event
					try {
						Logging.logger.trace("Message received: " + jsonMessage);
						Notification notify = new Notification(jsonMessage);
						Logging.logger.trace("Notification: " + notify);
						handler.onSemanticEvent(notify);
					} catch (Exception e) {
						Logging.logger.error("Exception on handling onSemanticEvent. Handler: " + handler + " Exception: "
								+ e.getMessage());
					}
				} else if (jsonMessage.has("error")) {
					ErrorResponse error = new ErrorResponse(jsonMessage.get("status_code").getAsInt(),
							jsonMessage.get("error").getAsString(), jsonMessage.get("error_description").getAsString());
					Logging.logger.error(error);
					try {
						handler.onError(error);
					} catch (Exception e) {
						e.printStackTrace();
						Logging.logger.error(
								"Exception on handling onError. Handler: " + handler + " Exception: " + e.getMessage());
					}
				} else if (jsonMessage.has("unsubscribed")) {
					Logging.logger.debug("unsubscribed");
					try {
						handler.onUnsubscribe(
								jsonMessage.get("unsubscribed").getAsJsonObject().get("spuid").getAsString());
					} catch (Exception e) {
						Logging.logger.error("Exception on handling onUnsubscribe. Handler: " + handler + " Exception: "
								+ e.getMessage());
					}
				} else
					Logging.logger.error("Unknown message: " + message);
			}
		});
	}

	public void onClose(Session session, CloseReason closeReason) {
		Logging.logger.warn("onClose session: " + session + " reason: " + closeReason);

		try {
			handler.onBrokenConnection(new ErrorResponse(closeReason.getCloseCode().getCode(), closeReason.getCloseCode().toString(), closeReason.getReasonPhrase()));
		} catch (Exception e) {
			Logging.logger.error(
					"Exception on handling onBrokenConnection. Handler: " + handler + " Exception: " + e.getMessage());
		}
	}

	public void onError(Session session, Throwable thr) {
		// Parsing error as JSON
		String msg = thr.getMessage();
		ErrorResponse error = null;
		try{
			msg = msg.substring(0, msg.lastIndexOf('}')+1);
			JsonObject err = new Gson().fromJson(msg, JsonObject.class);
			error = new ErrorResponse(err.get("status_code").getAsInt(), err.get("error").getAsString(), err.get("error_description").getAsString());
		}
		catch(JsonParseException e) {
			error = new ErrorResponse(500, "Exception", msg);
		}
		
		if (error.getStatusCode() != 1000) {
			Logging.logger.error(error);
			try {
				handler.onError(error);
			} catch (Exception e) {
				Logging.logger.error("Exception on handling onError. Handler: " + handler + " Exception: " + e.getMessage());
			}
		}
		else Logging.logger.warn(error);

	}

	public boolean isConnected() {
		if (session == null)
			return false;

		return session.isOpen();
	}

	public void send(String subscribeRequest) throws SEPAProtocolException {
		try {
			session.getBasicRemote().sendText(subscribeRequest);
			return;
		} catch (IOException e) {
			throw new SEPAProtocolException(e);
		}
		
	}

}
