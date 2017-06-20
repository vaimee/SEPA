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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SSLSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class Websocket extends WebSocketClient {
	protected Logger logger = LogManager.getLogger("WebsocketClientEndpoint");
	
	private String sparql;
	private String jwt;
	private String alias;
	
	private INotificationHandler handler;
	//private Watchdog watchDog = null;
	
	public Websocket(String wsUrl,boolean ssl) throws URISyntaxException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
		super(new URI(wsUrl));
		if (ssl) {
			SSLSecurityManager sm = new SSLSecurityManager("TLS","sepa.jks","sepa2017","sepa2017");
			setSocket(sm.createSSLSocket());
		}
	}
	
	private void sendSubscribeRequest() {
		logger.debug("@sendRequest");
		if (!isOpen()) return;
		
		JsonObject request = new JsonObject();
		request.add("subscribe", new JsonPrimitive(sparql));
		if (alias != null) request.add("alias", new JsonPrimitive(alias));
		else logger.debug("Alias is null");
		if (jwt != null) request.add("authorization", new JsonPrimitive("Bearer "+jwt));
		else logger.debug("Authorization is null");
		logger.debug(request.toString());
		
		send(request.toString());	
	}
	
	public void subscribe(String sparql,String alias,String jwt,INotificationHandler handler) throws IOException, URISyntaxException {
		logger.debug("@subscribe");
		
		this.handler = handler;
		this.sparql = sparql;
		this.alias = alias;
		this.jwt = jwt;
			
		if (!isOpen()) connect();
		else sendSubscribeRequest();
	}
	
	public void unsubscribe(String spuid,String jwt) throws IOException, URISyntaxException {
		logger.debug("unsubscribe");
		
		if (!isOpen()) return;
		else {
		
			JsonObject request = new JsonObject();
			if (spuid != null) request.add("unsubscribe", new JsonPrimitive(spuid));
			if (jwt != null) request.add("authorization", new JsonPrimitive("Bearer "+jwt));
			
			send(request.toString());
		}
	}

	@Override
	public void onMessage(String message) {
		logger.debug(message);
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
	  	  		
	  	  		//watchDog.ping();
	  	  		return;
	  	  	}
			 
	  	  	//Subscribe confirmed
	  	  	if (notify.get("subscribed") != null) {
	  	  		SubscribeResponse response;
	  	  		if (notify.get("alias") != null) response = new SubscribeResponse(0,notify.get("subscribed").getAsString(),notify.get("alias").getAsString());
	  	  		else response = new SubscribeResponse(0,notify.get("subscribed").getAsString());
	
	  	  		handler.onSubscribeConfirm(response);
	  	  	
//	  	  		if (!watchDog.isAlive()) watchDog.start();
//	  	  		watchDog.subscribed();
	  	  		return;
	  	  	}
	  	  	
	  	  	//Unsubscribe confirmed
	  	  	if (notify.get("unsubscribed") != null) {
	  	  		handler.onUnsubscribeConfirm(new UnsubscribeResponse(0,notify.get("unsubscribed").getAsString()));
	  	  		
//	  	  		//super.close();	  	  	
//	  	  		watchDog.unsubscribed();
	  	  		return;
	  	  	}
	  	  	
	  	  	//Notification
	  	  	if (notify.get("results") != null) handler.onSemanticEvent(new Notification(notify));	
		}
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		logger.debug("@onClose");	
	}

	@Override
	public void onError(Exception arg0) {
		logger.debug("@onError: "+arg0.getLocalizedMessage());
		
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		logger.debug("@onOpen");
		sendSubscribeRequest();
		//Start watchdog
		//if (watchDog == null) watchDog = new Watchdog(handler,this,sparql); 
	}
}
