/* This class is a Websocket implementation of the SPARQL 1.1 SE Protocol
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.ProtocolHandler;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketListener;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.scheduling.RequestResponseHandler.ResponseAndNotificationListener;

//import arces.unibo.SEPA.security.AuthorizationManager;

/* SPARQL 1.1 Subscribe language 
 * 
 * {"subscribe":"SPARQL Query 1.1", "authorization": "JWT", "alias":"an alias for the subscription"}
 * 
 * {"unsubscribe":"SPUID", "authorization": "JWT"}
 * 
 * If security is not required (i.e., ws), authorization key MAY be missing
 * */
public class WSGate extends WebSocketApplication {
	protected Logger logger = LogManager.getLogger("WebsocketGate");
	protected EngineProperties properties;
	protected Scheduler scheduler;
	
	//Collection of active sockets
	protected HashMap<WebSocket,SEPAResponseListener> activeSockets = new HashMap<WebSocket,SEPAResponseListener>();
	
	@Override
	public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket requestPacket,WebSocketListener... listeners) {
		WebSocket ret = super.createSocket(handler, requestPacket, listeners);
		
		logger.debug("@createSocket");
	    logger.debug("Protocol : " + requestPacket.getProtocol().getProtocolString());
	    logger.debug("Local port : " + requestPacket.getLocalPort());
	    
		return ret;
	}

	@Override
	public void onClose(WebSocket socket, DataFrame frame) {
		super.onClose(socket, frame);
		
		logger.debug("@onClose");

		if (properties.getKeepAlivePeriod() == 0) activeSockets.get(socket).unsubscribeAll();
	}

	@Override
	public void onConnect(WebSocket socket) {
		super.onConnect(socket);
		
		logger.debug("@onConnect");
		
		SEPAResponseListener listener = new SEPAResponseListener(socket);
		
		synchronized(activeSockets) {
			activeSockets.put(socket, listener);
		}    
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		//super.onMessage(socket, text);
		
		logger.debug("@onMessage "+text);

		int token = scheduler.getToken();
		if (token == -1) {
			ErrorResponse response = new ErrorResponse(token,405,"No more tokens");			
			socket.send(response.toString());			
			return;
		}
		
		Request request = parseRequest(token,text);		
		if(request == null) {
			logger.debug("Not supported request: "+text);
			ErrorResponse response = new ErrorResponse(token,400,"Not supported request: ");		
			socket.send(response.toString());
			
			scheduler.releaseToken(token);
			return;
		}
			
		synchronized(activeSockets) {
			logger.debug(">> Scheduling request: "+request.toString());
			scheduler.addRequest(request,activeSockets.get(socket));	
		}
	}
	
	protected Request parseRequest(Integer token,String request) {
		JsonObject req;
		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			return null;
		}
		
		if (req.get("subscribe") != null) {
			if (req.get("alias") != null) return new SubscribeRequest(token,req.get("subscribe").getAsString(),req.get("alias").getAsString());
			return new SubscribeRequest(token,req.get("subscribe").getAsString());
		}
		if (req.get("unsubscribe") != null) return new UnsubscribeRequest(token,req.get("unsubscribe").getAsString());	
		
		return null;
	}
	
	public WSGate(EngineProperties properties,Scheduler scheduler) {
		if (scheduler == null) {
			logger.fatal("Scheduler is null");
			System.exit(1);
		}
		
		if (properties == null) {
			logger.fatal("Properties are null");
			System.exit(1);
		}
		
		this.properties = properties;
		this.scheduler = scheduler;
	}
	
	public boolean start(){	
		//Create an HTTP server to which attach the websocket
		final HttpServer server = HttpServer.createSimpleServer(null, properties.getSubscribePort());

		// Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
        		
        // register the application
        WebSocketEngine.getEngine().register("", properties.getSubscribePath(), this);
		
        //Start the server
        try {
			server.start();
		} catch (IOException e) {
			logger.fatal("Failed to start WebSocket gate on port "+properties.getSubscribePort()+ " "+e.getMessage());
			System.exit(1);
		}
        
        String host = "localhost";
	    try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
	    
	    logger.info("Listening for SPARQL SUBSCRIBE/UNSUBSCRIBE on ws://"+host+":"+properties.getSubscribePort()+properties.getSubscribePath());
		
		//Start the keep alive thread
		if (properties.getKeepAlivePeriod() > 0) new KeepAlive().start();
		
		return true;
	}
	
	public class SEPAResponseListener implements ResponseAndNotificationListener {
		private WebSocket socket;	
		private HashSet<String> spuIds = new HashSet<String>();
		
		public int activeSubscriptions() {
			return spuIds.size();
		}
		
		public void unsubscribeAll() {
			synchronized(spuIds) {
				Iterator<String> it = spuIds.iterator();
				
				while(it.hasNext()) {
					int token = scheduler.getToken();
					if (token == -1) {
						logger.error("No more tokens");
						continue;
					}
					logger.debug(">> Scheduling UNSUBSCRIBE request #"+token);
					scheduler.addRequest(new UnsubscribeRequest(token,it.next()),this);		
				}
			}
		}
		
		@Override
		public void notify(Response response) {		
			if (response.getClass().equals(SubscribeResponse.class)) {
				logger.debug("<< SUBSCRIBE response #"+response.getToken());
				
				synchronized(spuIds) {
					spuIds.add(((SubscribeResponse)response).getSpuid());
				}
			
			}else if(response.getClass().equals(UnsubscribeResponse.class)) {
				logger.debug("<< UNSUBSCRIBE response #"+response.getToken()+" ");
				
				synchronized(spuIds) {
					spuIds.remove(((UnsubscribeResponse)response).getSpuid());
				
					synchronized(activeSockets) {
						if (spuIds.isEmpty()) activeSockets.remove(socket);
					}
				}
			}
			
			//Send response to client
			if (socket != null) if (socket.isConnected()) socket.send(response.toString());	
			
			//Release token
			if (!response.getClass().equals(Notification.class)) scheduler.releaseToken(response.getToken());
		}
		
		public Set<String> getSPUIDs() {return spuIds;}
		
		public SEPAResponseListener(WebSocket socket) {this.socket = socket;}
	}
	
	public class KeepAlive extends Thread {
		public void run() {
			while(true) {
				try {Thread.sleep(properties.getKeepAlivePeriod());} 
				catch (InterruptedException e) {return;}
				
				//Send heart beat on each active socket to detect broken sockets				
				synchronized(activeSockets) {
					for(WebSocket socket : activeSockets.keySet()) {	
						//Send ping only on sockets with active subscriptions
						if (activeSockets.get(socket).activeSubscriptions() == 0) continue;
						
						if (socket.isConnected()) {
							Ping ping = new Ping();
							socket.send(ping.toString());
						}
						else activeSockets.get(socket).unsubscribeAll();
						
					}
				}					
			}
		}
	}
		
	
}
