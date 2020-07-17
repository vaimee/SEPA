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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

public class WebsocketSubscriptionProtocol extends SubscriptionProtocol implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	protected final URI url;
	protected Request lastRequest = null;
	protected final WebsocketClientEndpoint client;

	public WebsocketSubscriptionProtocol(String host, int port, String path, ISubscriptionHandler handler,
			ClientSecurityManager sm) throws SEPASecurityException, SEPAProtocolException {
		super(handler, sm);

		// Connect
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

		client = new WebsocketClientEndpoint(sm, this);
	}

	@Override
	public void subscribe(SubscribeRequest request) throws SEPAProtocolException {
		logger.trace("subscribe: " + request);

		synchronized (client) {
			if (lastRequest != null)
				try {
					logger.trace("WAIT");
					client.wait();
				} catch (InterruptedException e) {
					throw new SEPAProtocolException(e.getMessage());
				}

			lastRequest = request;

			if (!client.isConnected()) {
				logger.trace("CONNECT");
				client.connect(url);
			}

			logger.trace("SEND "+lastRequest.toString());
			client.send(lastRequest.toString());
		}
	}

	@Override
	public void unsubscribe(UnsubscribeRequest request) throws SEPAProtocolException {
		logger.trace("unsubscribe: " + request);

		synchronized (client) {
			if (lastRequest != null)
				try {
					client.wait();
				} catch (InterruptedException e) {
					throw new SEPAProtocolException(e.getMessage());
				}
			lastRequest = request;

			if (client.isConnected())
				client.send(lastRequest.toString());
		}
	}

	@Override
	public void close() throws IOException {
		logger.trace("Close");

		client.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		handler.onSemanticEvent(notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		handler.onBrokenConnection(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// REFRESH TOKEN
		if (sm != null && errorResponse.isTokenExpiredError()) {
			try {
				logger.trace("REFRESH TOKEN");
				Response ret = sm.refreshToken();
				sm.storeOAuthProperties();
				logger.trace(ret);
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				logger.error(e.getMessage());
				if (logger.isTraceEnabled())
					e.printStackTrace();
				ErrorResponse err = new ErrorResponse(401, "invalid_grant",
						"Failed to refresh token. " + e.getMessage());
				handler.onError(err);
				return;
			}

			// RETRY LAST REQUEST
			synchronized (client) {
				if (client.isConnected())
					try {
						if (lastRequest != null) {
							lastRequest.setAuthorizationHeader(sm.getAuthorizationHeader());
							logger.trace("SEND LAST REQUEST WITH NEW TOKEN");
							client.send(lastRequest.toString());
						}
					} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
						logger.error(e.getMessage());
						if (logger.isTraceEnabled())
							e.printStackTrace();
						ErrorResponse err = new ErrorResponse(401, "invalid_grant",
								"Failed to send request after refreshing token. " + e.getMessage());
						handler.onError(err);
					}
			}
		} else
			handler.onError(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.trace("@onSubscribe "+spuid+" alias: "+alias);
		handler.onSubscribe(spuid, alias);

		synchronized (client) {
			lastRequest = null;
			client.notify();
		}

	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.trace("@onUnsubscribe "+spuid);
		handler.onUnsubscribe(spuid);

		synchronized (client) {
			lastRequest = null;
			client.notify();
		}
	}

}
