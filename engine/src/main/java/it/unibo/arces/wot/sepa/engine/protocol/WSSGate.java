/* This class is a secure Websocket implementation of the SPARQL 1.1 SE Protocol
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

import org.glassfish.grizzly.http.server.HttpServer;

import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class WSSGate extends WSGate {
	public WSSGate(EngineProperties properties, Scheduler scheduler,AuthorizationManager am) {
		super(properties, scheduler);
		if (am == null) throw new IllegalArgumentException("Authorization manager can not be null");
		this.am = am;
	}

	private AuthorizationManager am;
	
	@Override
	public boolean start(){	
		//Create an HTTP server to which attach the websocket
		final HttpServer server = HttpServer.createSimpleServer(null, properties.getSecureSubscribePort());
		
		// Register the WebSockets add on with the HttpServer
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
        
        // Security settings
        server.getListener("grizzly").setSSLEngineConfig(am.getWssConfigurator());
        server.getListener("grizzly").setSecure(true);
		
        // register the application
        WebSocketEngine.getEngine().register("", properties.getSecureSubscribePath(), this);
		
        //Start the server
        try {
			server.start();
		} catch (IOException e) {
			logger.fatal("Failed to start SECURE WebSocket gate on port "+properties.getSecureSubscribePort()+ " "+e.getMessage());
			System.exit(1);
		}
        
        String host = "localhost";
	    try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
	    
	    logger.info("Listening for SECURE SPARQL SUBSCRIBE/UNSUBSCRIBE on wss://"+host+":"+properties.getSecureSubscribePort()+properties.getSecureSubscribePath());
		
		//Start the keep alive thread
		if (properties.getKeepAlivePeriod() > 0) new KeepAlive().start();
		
		return true;
	}
	
	@Override
	public void onMessage(WebSocket socket, String text) {
		logger.debug("@onMessage "+text);
		
		//JWT Validation		
		Response validation = validateToken(text);
		if (validation.getClass().equals(ErrorResponse.class)) {
			//Not authorized
			logger.warn("NOT AUTHORIZED");
			socket.send(validation.toString());		
			return;
		}
		
		super.onMessage(socket, text);	
	}
		
	private Response validateToken(String request) {
		JsonObject req;
		try{
			req = new JsonParser().parse(request).getAsJsonObject();
		}
		catch(JsonParseException | IllegalStateException e) {
			
			return new ErrorResponse(ErrorResponse.UNAUTHORIZED,e.getMessage());
		}
		
		if (req.get("authorization") == null) return new ErrorResponse(ErrorResponse.UNAUTHORIZED,"authorization key is missing");;
		
		//Token validation
		return 	am.validateToken(req.get("authorization").getAsString());
	}
}
