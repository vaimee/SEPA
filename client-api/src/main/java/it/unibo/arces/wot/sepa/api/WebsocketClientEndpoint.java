/* This class is part of the SPARQL 1.1 SE Protocol (an extension of the W3C SPARQL 1.1 Protocol) API
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

package it.unibo.arces.wot.sepa.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.tyrus.client.ClientManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class WebsocketClientEndpoint extends Endpoint implements MessageHandler.Whole<String> {
	protected Logger logger = LogManager.getLogger("WebsocketClientEndpoint");
	
	private Session wsClientSession = null;;
	protected ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
	protected ClientManager client = ClientManager.createClient();
	
	private String sparql;
	private String wsUrl;
	private String jwt;
	private String alias;
	
	private INotificationHandler handler;
	private WebsocketWatchdog watchDog = null;
	
	public WebsocketClientEndpoint(String wsUrl) {
		this.wsUrl = wsUrl;
	}
	
	private void sendSubscribeRequest() {
		if (wsClientSession == null) {
			logger.error("Session is null");
			return;
		}
		
		try {
    		JsonObject request = new JsonObject();
			request.add("subscribe", new JsonPrimitive(sparql));
			if (alias != null) request.add("alias", new JsonPrimitive(alias));
			else logger.debug("Alias is null");
			if (jwt != null) request.add("authorization", new JsonPrimitive(jwt));
			else logger.debug("Authorization is null");
			logger.debug(request.toString());
			
			wsClientSession.getBasicRemote().sendText(request.toString());
		} 
		catch (IOException e) {
			logger.error(e.getMessage());
		}	
	}
	
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		logger.debug("@onOpen");
		
		wsClientSession = session;
    	wsClientSession.addMessageHandler(this);	
    	
    	sendSubscribeRequest();
	}

	private void connect() throws DeploymentException, IOException, URISyntaxException {
		logger.debug("Connect to server: "+wsUrl);
		
		client.connectToServer(this,cec, new URI(wsUrl));
	}
	
	boolean isConnected() {
		if (wsClientSession == null) return false;
		return wsClientSession.isOpen();
	}
	
	public void subscribe(String sparql,String alias,String jwt,INotificationHandler handler) throws IOException, URISyntaxException {
		this.handler = handler;
		this.sparql = sparql;
		this.alias = alias;
		this.jwt = jwt;
		
		if (!isConnected())
			try {
				connect();
			} catch (DeploymentException e) {
				throw new IOException(e.getMessage());
			}
		else sendSubscribeRequest();
		
		//Start watchdog
		if (watchDog == null) watchDog = new WebsocketWatchdog(handler,this,sparql); 
	}
	
	public void unsubscribe(String spuid,String jwt) throws IOException, URISyntaxException {
		logger.debug("unsubscribe");
		
		if (!isConnected())
			try {
				connect();
			} catch (DeploymentException e) {
				throw new IOException(e.getMessage());
			}
		
			JsonObject request = new JsonObject();
			if (spuid != null) request.add("unsubscribe", new JsonPrimitive(spuid));
			if (jwt != null) request.add("authorization", new JsonPrimitive(jwt));
			
			wsClientSession.getBasicRemote().sendText(request.toString());
	}
	
	public boolean close() {
		try {
			wsClientSession.close();
		} catch (IOException e) {
			logger.debug(e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void onMessage(String message) {
		logger.debug("Message: "+message);
		if (handler == null) {
			logger.warn("Notification handler is NULL");
			return;
		}
  	  	
		synchronized(handler) {
			JsonObject notify = new JsonParser().parse(message).getAsJsonObject();
			
			//Error
			if (notify.get("code") != null) {
				ErrorResponse error;
				if (notify.get("body") != null) error = new ErrorResponse(notify.get("code").getAsInt(),notify.get("body").getAsString());
				else error = new ErrorResponse(notify.get("code").getAsInt(),"");
					
				handler.onError(error);
				return;
			}
			
	  	  	//Ping
	  	  	if(notify.get("ping") != null) {
	  	  		handler.onPing();
	  	  		
	  	  		watchDog.ping();
	  	  		return;
	  	  	}
			 
	  	  	//Subscribe confirmed
	  	  	if (notify.get("subscribed") != null) {
	  	  		SubscribeResponse response;
	  	  		if (notify.get("alias") != null) response = new SubscribeResponse(0,notify.get("subscribed").getAsString(),notify.get("alias").getAsString());
	  	  		else response = new SubscribeResponse(0,notify.get("subscribed").getAsString());
	
	  	  		handler.onSubscribeConfirm(response);
	  	  	
	  	  		if (!watchDog.isAlive()) watchDog.start();
	  	  		watchDog.subscribed();
	  	  		return;
	  	  	}
	  	  	
	  	  	//Unsubscribe confirmed
	  	  	if (notify.get("unsubscribed") != null) {
	  	  		handler.onUnsubscribeConfirm(new UnsubscribeResponse(0,notify.get("unsubscribed").getAsString()));
	  	  		
	  	  		try {
					wsClientSession.close();
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
	  	  	
	  	  		watchDog.unsubscribed();
	  	  		return;
	  	  	}
	  	  	
	  	  	//Notification
	  	  	if (notify.get("results") != null) handler.onSemanticEvent(new Notification(notify));	
		}
	}
}
