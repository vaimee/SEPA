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

package it.unibo.arces.wot.sepa.api.protocols.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
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
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

public class WebsocketSubscriptionProtocol extends Endpoint implements SubscriptionProtocol {
	protected final Logger logger = LogManager.getLogger();

	protected final String host;
	protected int port = -1;
	protected final String path;

	protected ISubscriptionHandler handler = null;
	protected SEPASecurityManager sm = null;

	protected final ClientManager client;
	protected Session session;
	protected String subscribeRequest;

	public WebsocketSubscriptionProtocol(String host, int port, String path) {
		this.host = host;
		this.port = port;
		this.path = path;

		client = ClientManager.createClient();
	}

	public WebsocketSubscriptionProtocol(String host, String path) {
		this.host = host;
		this.path = path;

		client = ClientManager.createClient();
	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		logger.trace("@subscribe: " + request);

		// Check is socket is open
		if (session != null)
			if (session.isOpen()) {
				try {
					session.getBasicRemote().sendText(request.toString());
					return;
				} catch (IOException e) {
					throw new SEPAProtocolException(e);
				}
			}

		// Connect
		URI url = null;
		String scheme = "ws://";
		if (sm != null)
			scheme = "wss://";
		if (port == -1)
			try {
				url = new URI(scheme + host + path);
			} catch (URISyntaxException e) {
				logger.error(e.getMessage());
				throw new SEPAProtocolException(e);
			}
		else
			try {
				url = new URI(scheme + host + ":" + port + path);
			} catch (URISyntaxException e) {
				logger.error(e.getMessage());
				throw new SEPAProtocolException(e);
			}

		// Attempt up to X times to connect
		int retries = 5;
		while (true) {
			try {
				subscribeRequest = request.toString();

				logger.debug("Connect to: " + url);

				client.connectToServer(this, url);

				return;
				
			} catch (DeploymentException | IOException e) {
				logger.error(e.getMessage()+" Retries: "+retries);
				retries--;
				if (retries == 0) throw new SEPAProtocolException(e);
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					throw new SEPAProtocolException(e1);
				}
			}
		}

	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		logger.debug("@unsubscribe: " + request);

		if (session != null)
			try {
				session.getBasicRemote().sendText(request.toString());
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new SEPAProtocolException(e);
			}
	}

	@Override
	public void close() throws IOException {
		logger.trace("Close");
		if (session != null)
			session.close();
	}

	@Override
	public void setHandler(ISubscriptionHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("Handler is null");

		this.handler = handler;
	}

	@Override
	public void enableSecurity(SEPASecurityManager sm) throws SEPASecurityException {
		if (sm == null)
			throw new IllegalArgumentException("Security manager is null");

		this.sm = sm;

		SslEngineConfigurator config = new SslEngineConfigurator(sm.getSSLContext());
		config.setHostVerificationEnabled(false);
		client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, config);
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		logger.debug("@onOpen session: " + session.getId());

		this.session = session;

		session.addMessageHandler(String.class, new MessageHandler.Whole<String>() {
			@Override
			public void onMessage(String message) {
				logger.trace("@onMessage: " + message);

				// Parse message
				JsonObject jsonMessage = null;
				try {
					jsonMessage = new JsonParser().parse(message).getAsJsonObject();
				} catch (Exception e) {
					logger.error("Exception on parsing message: "+message+" exception: "+e.getMessage());
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
							logger.error("Exception on handling onSubscribe. Handler: "+handler+" Exception: " + e.getMessage());
							return;
						}
					}

					// Event
					try {
						logger.trace("Message received: "+jsonMessage);
						Notification notify = new Notification(jsonMessage);
						logger.trace("Notification: " + notify);
						handler.onSemanticEvent(notify);
					} catch (Exception e) {
						logger.error("Exception on handling onSemanticEvent. Handler: "+handler+" Exception: " + e.getMessage());
					}
				} else if (jsonMessage.has("error")) {
					ErrorResponse error = new ErrorResponse(jsonMessage.get("status_code").getAsInt(),
							jsonMessage.get("error").getAsString(), jsonMessage.get("error_description").getAsString());
					logger.error(error);
					try {
						handler.onError(error);
					} catch (Exception e) {
						logger.error("Exception on handling onError. Handler: "+handler+" Exception: " + e.getMessage());
					}
				} else if (jsonMessage.has("unsubscribed")) {
					logger.debug("unsubscribed");
					try {
						handler.onUnsubscribe(
								jsonMessage.get("unsubscribed").getAsJsonObject().get("spuid").getAsString());
					} catch (Exception e) {
						logger.error("Exception on handling onUnsubscribe. Handler: "+handler+" Exception: " + e.getMessage());
					}
				} else
					logger.error("Unknown message: " + message);
			}
		});

		try {
			session.getBasicRemote().sendText(subscribeRequest);
		} catch (IOException e) {
			logger.error("onOpen send subscribe request: " + e.getMessage());
		}

	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		logger.warn("onClose session: " + session + " reason: " + closeReason);

		try {
			handler.onBrokenConnection();
		} catch (Exception e) {
			logger.error("Exception on handling onBrokenConnection. Handler: "+handler+" Exception: " + e.getMessage());
		}
	}

	@Override
	public void onError(Session session, Throwable thr) {
		ErrorResponse error = new ErrorResponse(500, "Exception", thr.getMessage());
		logger.error("@onError: " + error);

		try {
			handler.onError(error);
		} catch (Exception e) {
			logger.error("Exception on handling onError. Handler: "+handler+" Exception: " + e.getMessage());
		}
	}

}
