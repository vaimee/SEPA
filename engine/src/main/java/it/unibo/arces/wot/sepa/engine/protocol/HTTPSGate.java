/* This class implements part of the SPARQL 1.1 SE Protocol using the secure HTTPS protocol
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
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.List;

import org.apache.commons.io.IOUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.security.CORSManager;

public class HTTPSGate extends HTTPGate implements HTTPSGateMBean {
	protected Logger logger = LogManager.getLogger("HTTPSGate");	
	protected String mBeanName = "SEPA:type=HTTPSGate";
		
	//Authorization manager
	protected HttpServer authorizationServer = null;
	private AuthorizationManager am;

	private long registrationTransactions = 0;
	private long requestTokenTransactions = 0;
	private long httpsTotalRequests =0 ;

	/*
	Error Code	Description (RFC 2616 Status codes) 
	
	400			Bad Request
	401			Unauthorized
	403			Forbidden
	404			Not Found
	405			Method Not Allowed
	429			Too Many Requests
	500			Internal Server Error
	503			Service Unavailable
	*/
	
	public HTTPSGate(EngineProperties properties, SchedulerInterface scheduler,AuthorizationManager am) throws IllegalArgumentException {
		super(properties, scheduler);
		if (am == null) throw new IllegalArgumentException("Authorization manager can not be null");
		this.am = am;
	}
	
	/**
	 * Registration is done according [RFC7591] and described in the following.
	 * Create a HTTP request with JSON request content as in the following prototype and send it via TLS to the AM.
	 * 
	 * Request 
	 * POST HTTP/1.1
	 * 
	 * Request headers 
	 * Host: <URL> 
	 * Content-Type: application/json 
	 * Accept: application/json
	 * 
	 * Request body 
	 * { 
	 * "client_identity": "IDENTITY", 
	 * "grant_types": ["client_credentials"] 
	 * }
	 */
	class RegistrationHandler extends SPARQLHandler {

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			logger.info(">> HTTPS request (REGISTRATION)");
			registrationTransactions++;
			
			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange,ErrorResponse.UNAUTHORIZED,"CORS origin not allowed");
				return;
			}
			
			if (CORSManager.isPreFlightRequest(exchange)) {
				sendResponse(exchange,204,null);
				return;
			}
			
			String name = null;
			
			try {
				// Parsing and validating request headers
				// Content-Type: application/json 
				// Accept: application/json
				if (!exchange.getRequestHeaders().get("Content-Type").contains("application/json")) {
					logger.error("Bad request header: Content-Type must be <application/json>");
					failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Content-Type must be <application/json>");
					return;	
				}	
				if (!exchange.getRequestHeaders().get("Accept").contains("application/json")) {
					logger.error("Bad request header: Accept must be <application/json>");
					failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Accept must be <application/json>");
					return;	
				}
				
				//Parsing and validating request body
				/*{ 
					"client_identity": "IDENTITY", 
					"grant_types": ["client_credentials"] 
				}*/
				String jsonString = IOUtils.toString(exchange.getRequestBody(),"UTF-8");
				JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
				JsonArray credentials = json.get("grant_types").getAsJsonArray();
				boolean found = false;
				for (JsonElement elem: credentials){
					if (elem.getAsString() != null)
						if (elem.getAsString().equals("client_credentials")) {
							found = true;
							break;
						}
				}
				if(!found) {
					logger.error("\"grant_types\" must contain \"client_credentials\"");
					failureResponse(exchange,400,"\"grant_types\" must contain \"client_credentials\"");
					return;	
				}
				name = json.get("client_identity").getAsString();
			}
			catch(NullPointerException e) {
				logger.error(e.getMessage());
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,e.getMessage());
				return;		
			}
			
			/*
			if (!exchange.getRequestHeaders().containsKey("Content-Type")) {
				logger.error("Bad request header: Content-Type is missing");
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Content-Type is missing");
				return;
			}
			if (!exchange.getRequestHeaders().get("Content-Type").contains("application/json")) {
				logger.error("Bad request header: Content-Type must be <application/json>");
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Content-Type must be <application/json>");
				return;	
			}
			
			if (!exchange.getRequestHeaders().containsKey("Accept")) {
				logger.error("Bad request header: Accept is missing");
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Accept is missing");
				return;
			}
			if (!exchange.getRequestHeaders().get("Accept").contains("application/json")) {
				logger.error("Bad request header: Accept must be <application/json>");
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,"Bad request header: Accept must be <application/json>");
				return;	
			}
			*/
			
			//Parsing and validating request body
			/*{ 
				"client_identity": "IDENTITY", 
				"grant_types": ["client_credentials"] 
			}*/
			/*String name = null;
			try {
				String jsonString = IOUtils.toString(exchange.getRequestBody(),"UTF-8");
				JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
				JsonArray credentials = json.get("grant_types").getAsJsonArray();
				if(credentials.contains(new JsonPrimitive("client_credentials"))) {
					failureResponse(exchange,ErrorResponse.BAD_REQUEST,"<client_credentials> grant not found");
					return;	
				}
				name = json.get("client_identity").getAsString();
			
			} catch (IOException | JsonParseException | NullPointerException e) {
				logger.error(e.getMessage());
				failureResponse(exchange,ErrorResponse.BAD_REQUEST,e.getMessage());
				return;
			}
			*/
			
			/*
			if (json == null) {
				logger.error("Request body must be a JSON object");
				failureResponse(exchange,400,"Request body must be a JSON object");
				return;	
			}
			
			JsonArray credentials = json.get("grant_types").getAsJsonArray();
			if (credentials == null) {
				logger.error("Request body must contain: \"grant_types\"");
				failureResponse(exchange,400,"Request body must contain: \"grant_types\"");
				return;		
			}
			if (credentials.size() == 0) {
				logger.error("\"grant_types\" is null");
				failureResponse(exchange,400,"\"grant_types\" is null");
				return;		
			}
			boolean found = false;
			for (JsonElement elem: credentials){
				if (elem.getAsString() != null)
					if (elem.getAsString().equals("client_credentials")) {
						found = true;
						break;
					}
			}
			if(!found) {
				logger.error("\"grant_types\" must contain \"client_credentials\"");
				failureResponse(exchange,400,"\"grant_types\" must contain \"client_credentials\"");
				return;	
			}
			
			String name = json.get("client_identity").getAsString();
			if (name == null) {
				logger.error("JSON request body must contain: \"client_identity\"");
				failureResponse(exchange,400,"JSON request body must contain: \"client_identity\"");
				return;		
			}
			if (name.equals("")) {
				logger.error("\"client_identity\" is null");
				failureResponse(exchange,400,"\"client_identity\" is null");
				return;		
			}
			*/
			
			//*****************************************
			//Register client and retrieve credentials
			//*****************************************
			Response cred = am.register(name);
			
			if (cred.getClass().equals(ErrorResponse.class)) {
				logger.error(cred.toString());
				ErrorResponse error = (ErrorResponse) cred;
				failureResponse(exchange,error.getErrorCode(),error.getErrorMessage());
				return;
			}
			
			sendResponse(exchange,201,cred.toString());	
		}
	}
	
	/**
	 * Token Acquisition
	 * Create a HTTP request as in the following prototype and send it via TLS to the AM.
	 * 
	 * Request 
	 * POST HTTP/1.1
	 * 
	 * Request headers 
	 * Host: <URL> 
	 * Content-Type: application/json
	 * Accept: application/json 
	 * Authorization: Basic Base64(<c_id>:<c_secret>)
	 * 
	 * Request body 
	 * 
	 * { 
	 * "client_identity": "68:a8:6d:1a:9c:04", 
	 * "grant_types": ["client_credentials"] 
	 * }
	 * */
	class TokenHandler extends SPARQLHandler {
		
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			logger.info(">> HTTPS request (TOKEN REQUEST)");
			requestTokenTransactions++;
			
			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange,ErrorResponse.UNAUTHORIZED,"CORS origin not allowed");
				return;
			}
			
			if (CORSManager.isPreFlightRequest(exchange)) {
				sendResponse(exchange,204,null);
				return;
			}
			
			//Parsing and validating request headers
			// Content-Type: application/json 
			// Accept: application/json
			if (!exchange.getRequestHeaders().containsKey("Content-Type")) {
				logger.error("Bad request header: Content-Type is missing");
				failureResponse(exchange,400,"Bad request header: Content-Type is missing");
				return;
			}
			if (!exchange.getRequestHeaders().get("Content-Type").contains("application/json")) {
				logger.error("Bad request header: Content-Type must be \"application/json\"");
				failureResponse(exchange,400,"Bad request header: Content-Type must be \"application/json\"");
				return;	
			}
			
			if (!exchange.getRequestHeaders().containsKey("Accept")) {
				logger.error("Bad request header: Accept is missing");
				failureResponse(exchange,400,"Bad request header: Accept is missing");
				return;
			}
			if (!exchange.getRequestHeaders().get("Accept").contains("application/json")) {
				logger.error("Bad request header: Accept must be \"application/json\"");
				failureResponse(exchange,400,"Bad request header: Accept must be \"/application/json\"");
				return;	
			}
			
			//Authorization header
			if (!exchange.getRequestHeaders().containsKey("Authorization")) {
				logger.error("Authorization is null");
				failureResponse(exchange,401,"Authorization is null");
				return;
			}
			
			//Extract Basic64 authorization
			List<String> basic = exchange.getRequestHeaders().get("Authorization");
			if (basic.size()!=1) {
				logger.error("Basic is null");
				failureResponse(exchange,401,"Basic is null");
				return;		
			}
			if (!basic.get(0).startsWith("Basic ")) {
				logger.error("Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
				failureResponse(exchange,401,"Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
				return;		
			}
			
			//*************
			//Get token
			//*************
			Response token = am.getToken(basic.get(0).split(" ")[1]);
			
			if (token.getClass().equals(ErrorResponse.class)) {
				ErrorResponse error = (ErrorResponse) token;
				logger.error(token.toString());
				failureResponse(exchange,error.getErrorCode(),error.getErrorMessage());
			}
			else {
				sendResponse(exchange,201,token.toString());
			}
		}
	}
	
	/**
	 * Operation when receiving a HTTP request at a protected endpoint
	 * 
		 * 1. Check if the request contains an Authorization header.
	 * 2. Check if the request contains an Authorization: Bearer-header with non-null/empty contents
	 * 3. Check if the value of the Authorization: Bearer-header is a JWT object
	 * 4. Check if the JWT object is signed
	 * 5. Check if the signature of the JWT object is valid. This is to be checked with AS public signature verification key
	 * 6. Check the contents of the JWT object
	 * 7. Check if the value of "iss" is https://wot.arces.unibo.it:8443/oauth/token
	 * 8. Check if the value of "aud" contains https://wot.arces.unibo.it:8443/sparql
	 * 9. Accept the request as well as "sub" as the originator of the request and process it as usual
	 * 
	 * *** Respond with 401 if not
	 * */
	
	class SecureSPARQLHandler extends SPARQLHandler {
		
		@Override
		public void handle(HttpExchange httpExchange) throws IOException {
			logger.info(">> HTTPS request");	
			httpsTotalRequests ++;
			
			if (!CORSManager.processCORSRequest(httpExchange)) {
				failureResponse(httpExchange,ErrorResponse.UNAUTHORIZED,"CORS origin not allowed");
				return;
			}
			
			if (CORSManager.isPreFlightRequest(httpExchange)) {
				sendResponse(httpExchange,204,null);
				return;
			}
			
			//Extract Bearer authorization
			List<String> bearer = httpExchange.getRequestHeaders().get("Authorization");
			if (bearer == null) {
				logger.error("Authorization header is missing");
				failureResponse(httpExchange,ErrorResponse.UNAUTHORIZED,"Authorization header is missing");
				return;		
			}
			if (bearer.size()!=1) {
				logger.error("Too many authorization headers");
				failureResponse(httpExchange,ErrorResponse.UNAUTHORIZED,"Too many authorization headers");
				return;		
			}
			if (!bearer.get(0).startsWith("Bearer ")) {
				logger.error("Authorization must be \"Bearer JWT\"");
				failureResponse(httpExchange,ErrorResponse.UNAUTHORIZED,"Authorization must be in the form \"Bearer JWT\"");
				return;		
			}
			
			//******************
			//JWT validation
			//******************
			String jwt = bearer.get(0).split(" ")[1];
			
			Response valid = am.validateToken(jwt);
			if(!valid.getClass().equals(ErrorResponse.class)) 
				//Handle the request as a normal HTTP request
				super.handle(httpExchange);
			else {
				ErrorResponse error = (ErrorResponse) valid;
				failureResponse(httpExchange,error.getErrorCode(),error.getErrorMessage());
			}
		}
	}
	
	@Override
	public void start() {	
		this.setName("SEPA HTTPS Gate");
		
		SEPABeans.registerMBean(mBeanName,this);
		
		// create HTTPS server
		try {
			updateServer = HttpsServer.create(new InetSocketAddress(properties.getSecureUpdatePort()), 0);
			if (properties.getSecureUpdatePort() != properties.getSecureQueryPort()){
				queryServer = HttpsServer.create(new InetSocketAddress(properties.getSecureQueryPort()), 0);
				if (properties.getAuthorizationServerPort() == properties.getSecureUpdatePort()) authorizationServer = updateServer;
				else if (properties.getAuthorizationServerPort() == properties.getSecureQueryPort()) authorizationServer = queryServer;
				else authorizationServer = HttpsServer.create(new InetSocketAddress(properties.getAuthorizationServerPort()), 0);
			}
			else {
				queryServer = updateServer;
				if (properties.getAuthorizationServerPort() == properties.getSecureUpdatePort()) authorizationServer = updateServer;
				else {
					authorizationServer = HttpsServer.create(new InetSocketAddress(properties.getAuthorizationServerPort()), 0);	
				}
			}
		} catch (IOException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}
		
		// Set security configuration
		((HttpsServer)updateServer).setHttpsConfigurator(am.getHttpsConfigurator());
		if (queryServer != updateServer) 
			((HttpsServer)queryServer).setHttpsConfigurator(am.getHttpsConfigurator());
		if (authorizationServer != updateServer && authorizationServer != queryServer) 
			((HttpsServer)authorizationServer).setHttpsConfigurator(am.getHttpsConfigurator());
		
		//Update and query
	    updateServer.createContext(properties.getSecureUpdatePath(), new SecureSPARQLHandler());
	    updateServer.createContext("/echo", new EchoHandler());
	    if (queryServer != updateServer) {
	    	queryServer.createContext(properties.getSecureQueryPath(), new SecureSPARQLHandler());
	    	queryServer.createContext("/echo", new EchoHandler());
	    }
	    	    
	    //WoT Authentication
	    authorizationServer.createContext(properties.getRegisterPath(), new RegistrationHandler());
	    authorizationServer.createContext(properties.getTokenRequestPath(), new TokenHandler());
	    
	    //Starting...
	    updateServer.setExecutor(null);
	    updateServer.start();
	    
	    if (queryServer != updateServer) {
	    	queryServer.setExecutor(null);
	    	queryServer.start();
	    }
	    
	    if (authorizationServer != updateServer && authorizationServer != queryServer) {
	    	authorizationServer.setExecutor(null);
	    	authorizationServer.start();
	    }
	    
	    logger.info("HTTPS gate started");
	    
	    String host = "localhost";
	    try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}
	    
	    logger.info("Listening for SECURE SPARQL UPDATES on https://"+host+":"+properties.getSecureUpdatePort()+properties.getSecureUpdatePath());
	    logger.info("Listening for SECURE SPARQL QUERIES on https://"+host+":"+properties.getSecureQueryPort()+properties.getSecureQueryPath());
	    logger.info("Listening for REGISTRATION REQUESTS on https://"+host+":"+properties.getAuthorizationServerPort()+properties.getRegisterPath());
	    logger.info("Listening for TOKEN REQUESTS on https://"+host+":"+properties.getAuthorizationServerPort()+properties.getTokenRequestPath());
	}

	@Override
	public long getRegistrationTransactions() {
		return registrationTransactions;
	}

	@Override
	public long getRequestTokenTransactions() {
		return requestTokenTransactions;
	}

	@Override
	public long getSecureSPARQLTransactions() {
		return httpsTotalRequests;
	}
}