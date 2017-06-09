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

import java.net.UnknownHostException;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

	// Authorization manager
	protected HttpServer authorizationServer = null;
	private AuthorizationManager am;

	private long registrationTransactions = 0;
	private long requestTokenTransactions = 0;
	private long httpsTotalRequests = 0;

	private ServerBootstrap updateServerBoot = ServerBootstrap.bootstrap();
	private ServerBootstrap queryServerBoot = ServerBootstrap.bootstrap();
	private ServerBootstrap authorizationServerBoot = ServerBootstrap.bootstrap();
	
	/*
	 * Error Code Description (RFC 2616 Status codes)
	 * 
	 * 400 Bad Request 401 Unauthorized 403 Forbidden 404 Not Found 405 Method
	 * Not Allowed 429 Too Many Requests 500 Internal Server Error 503 Service
	 * Unavailable
	 */

	public HTTPSGate(EngineProperties properties, SchedulerInterface scheduler, AuthorizationManager am)
			throws IllegalArgumentException {
		super(properties, scheduler);

		if (am == null)
			throw new IllegalArgumentException("Authorization manager can not be null");
		this.am = am;

		// Update server
		updateServerBoot.registerHandler(properties.getSecureUpdatePath(),
				new SecureSPARQLHandler(properties.getSecureUpdatePath()));
		updateServerBoot.registerHandler("/echo", new EchoHandler());
		updateServerBoot.setListenerPort(properties.getSecureUpdatePort());
		updateServerBoot.setSslContext(am.getSSLContext());

		// Query server
		if (properties.getSecureUpdatePort() == properties.getSecureQueryPort())
			queryServerBoot = updateServerBoot;
		else {
			queryServerBoot.setListenerPort(properties.getSecureQueryPort());
			queryServerBoot.registerHandler("/echo", new EchoHandler());
			queryServerBoot.setSslContext(am.getSSLContext());
		}

		queryServerBoot.registerHandler(properties.getSecureQueryPath(),
				new SecureSPARQLHandler(properties.getSecureQueryPath()));

		// Authorization server
		if (properties.getAuthorizationServerPort() == properties.getSecureUpdatePort())
			authorizationServerBoot = updateServerBoot;
		else if (properties.getAuthorizationServerPort() == properties.getSecureQueryPort())
			authorizationServerBoot = queryServerBoot;
		else {
			authorizationServerBoot.setListenerPort(properties.getAuthorizationServerPort());
			authorizationServerBoot.registerHandler("/echo", new EchoHandler());
			authorizationServerBoot.setSslContext(am.getSSLContext());
		}

		authorizationServerBoot.registerHandler(properties.getRegisterPath(),
				new RegistrationHandler(properties.getRegisterPath()));
		authorizationServerBoot.registerHandler(properties.getTokenRequestPath(),
				new TokenHandler(properties.getTokenRequestPath()));
	}

	/**
	 * Registration is done according [RFC7591] and described in the following.
	 * Create a HTTP request with JSON request content as in the following
	 * prototype and send it via TLS to the AM.
	 * 
	 * Request POST HTTP/1.1
	 * 
	 * Request headers Host: <URL> Content-Type: application/json Accept:
	 * application/json
	 * 
	 * Request body { "client_identity": "IDENTITY", "grant_types":
	 * ["client_credentials"] }
	 */
	class RegistrationHandler extends SPARQLHandler {

		public RegistrationHandler(String updatePath) {
			super(updatePath);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange exchange, HttpContext context) {
			logger.info(">> HTTPS request (REGISTRATION)");
			registrationTransactions++;

			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED, "CORS origin not allowed");
				return;
			}

			if (CORSManager.isPreFlightRequest(exchange)) {
				sendResponse(exchange, 204, null);
				return;
			}

			String name = null;

			try {
				Header[] headers;
				// Parsing and validating request headers
				// Content-Type: application/json
				// Accept: application/json
				headers = request.getHeaders("Content-Type");
				if (headers.length == 0) {
					logger.error("Content-Type is missing");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Content-Type is missing");
				}
				if (headers.length > 1) {
					logger.error("Too many Content-Type headers");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Too many Content-Type headers");
				}
				if (!headers[0].getValue().equals("application/json")) {
					logger.error("Content-Type must be: application/json");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Content-Type must be: application/json");
				}

				headers = request.getHeaders("Accept");
				if (headers.length == 0) {
					logger.error("Accept is missing");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Accept is missing");
				}
				if (headers.length > 1) {
					logger.error("Too many Accept headers");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Too many Accept headers");
				}
				if (!headers[0].getValue().equals("application/json")) {
					logger.error("Accept must be: application/json");
					failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Accept must be: application/json");
				}

				// Parsing and validating request body
				/*
				 * { "client_identity": "IDENTITY", "grant_types":
				 * ["client_credentials"] }
				 */
				String jsonString = "";
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				try {
					jsonString = EntityUtils.toString(entity, Charset.forName("UTF-8"));
				} catch (ParseException | IOException e) {
					jsonString = e.getLocalizedMessage();
				}
				JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
				JsonArray credentials = json.get("grant_types").getAsJsonArray();
				boolean found = false;
				for (JsonElement elem : credentials) {
					if (elem.getAsString() != null)
						if (elem.getAsString().equals("client_credentials")) {
							found = true;
							break;
						}
				}
				if (!found) {
					logger.error("\"grant_types\" must contain \"client_credentials\"");
					failureResponse(exchange, 400, "\"grant_types\" must contain \"client_credentials\"");
					return;
				}
				name = json.get("client_identity").getAsString();
			} catch (NullPointerException e) {
				logger.error(e.getMessage());
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, e.getMessage());
				return;
			}

			// *****************************************
			// Register client and retrieve credentials
			// *****************************************
			Response cred = am.register(name);

			if (cred.getClass().equals(ErrorResponse.class)) {
				logger.error(cred.toString());
				ErrorResponse error = (ErrorResponse) cred;
				failureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
				return;
			}

			sendResponse(exchange, 201, cred.toString());
		}
	}

	/**
	 * Token Acquisition Create a HTTP request as in the following prototype and
	 * send it via TLS to the AM.
	 * 
	 * Request POST HTTP/1.1
	 * 
	 * Request headers Host: <URL> Content-Type: application/json Accept:
	 * application/json Authorization: Basic Base64(<c_id>:<c_secret>)
	 * 
	 * Request body
	 * 
	 * { "client_identity": "68:a8:6d:1a:9c:04", "grant_types":
	 * ["client_credentials"] }
	 */
	class TokenHandler extends SPARQLHandler {

		public TokenHandler(String updatePath) {
			super(updatePath);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange exchange, HttpContext context) {
			logger.info(">> HTTPS request (TOKEN REQUEST)");
			requestTokenTransactions++;

			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED, "CORS origin not allowed");
				return;
			}

			if (CORSManager.isPreFlightRequest(exchange)) {
				sendResponse(exchange, 204, null);
				return;
			}

			Header[] headers;
			// Parsing and validating request headers
			// Content-Type: application/json
			// Accept: application/json
			headers = request.getHeaders("Content-Type");
			if (headers.length == 0) {
				logger.error("Content-Type is missing");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Content-Type is missing");
			}
			if (headers.length > 1) {
				logger.error("Too many Content-Type headers");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Too many Content-Type headers");
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Content-Type must be: application/json");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Content-Type must be: application/json");
			}

			headers = request.getHeaders("Accept");
			if (headers.length == 0) {
				logger.error("Accept is missing");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Accept is missing");
			}
			if (headers.length > 1) {
				logger.error("Too many Accept headers");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Too many Accept headers");
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Accept must be: application/json");
				failureResponse(exchange, ErrorResponse.BAD_REQUEST, "Accept must be: application/json");
			}

			// Authorization header
			headers = request.getHeaders("Authorization");
			if (headers.length != 1) {
				logger.error("Authorization is missing or multiple");
				failureResponse(exchange, 401, "Authorization is missing or multiple");
				return;
			}

			// Extract Basic64 authorization
			String basic = headers[0].getValue();

			if (!basic.startsWith("Basic ")) {
				logger.error("Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
				failureResponse(exchange, 401, "Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
				return;
			}

			// *************
			// Get token
			// *************
			Response token = am.getToken(basic.split(" ")[1]);

			if (token.getClass().equals(ErrorResponse.class)) {
				ErrorResponse error = (ErrorResponse) token;
				logger.error(token.toString());
				failureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
			} else {
				sendResponse(exchange, 201, token.toString());
			}
		}
	}

	/**
	 * Operation when receiving a HTTP request at a protected endpoint
	 * 
	 * 1. Check if the request contains an Authorization header. 2. Check if the
	 * request contains an Authorization: Bearer-header with non-null/empty
	 * contents 3. Check if the value of the Authorization: Bearer-header is a
	 * JWT object 4. Check if the JWT object is signed 5. Check if the signature
	 * of the JWT object is valid. This is to be checked with AS public
	 * signature verification key 6. Check the contents of the JWT object 7.
	 * Check if the value of "iss" is
	 * https://wot.arces.unibo.it:8443/oauth/token 8. Check if the value of
	 * "aud" contains https://wot.arces.unibo.it:8443/sparql 9. Accept the
	 * request as well as "sub" as the originator of the request and process it
	 * as usual
	 * 
	 * *** Respond with 401 if not
	 */

	class SecureSPARQLHandler extends SPARQLHandler {

		public SecureSPARQLHandler(String updatePath) {
			super(updatePath);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange exchange, HttpContext context)
				throws HttpException, IOException {
			logger.info(">> HTTPS request");
			httpsTotalRequests++;

			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED, "CORS origin not allowed");
				return;
			}

			if (CORSManager.isPreFlightRequest(exchange)) {
				sendResponse(exchange, 204, null);
				return;
			}

			// Extract Bearer authorization
			Header[] bearer = exchange.getRequest().getHeaders("Authorization");

			if (bearer.length != 1) {
				logger.error("Authorization header is missing or multiple");
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED, "Authorization header is missing or multiple");
				return;
			}
			if (!bearer[0].getValue().startsWith("Bearer ")) {
				logger.error("Authorization must be \"Bearer JWT\"");
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED,
						"Authorization must be in the form \"Bearer JWT\"");
				return;
			}

			// ******************
			// JWT validation
			// ******************
			String jwt = bearer[0].getValue().split(" ")[1];

			Response valid = am.validateToken(jwt);
			if (!valid.getClass().equals(ErrorResponse.class))
				// Handle the request as a normal HTTP request
				super.handle(request, exchange, context);
			else {
				ErrorResponse error = (ErrorResponse) valid;
				failureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
			}
		}
	}

	@Override
	public void start() {
		this.setName("SEPA HTTPS Gate");

		SEPABeans.registerMBean(mBeanName, this);

		// Starting...
		try {
			updateServer = updateServerBoot.create();
			updateServer.start();
		} catch (IOException e1) {
			logger.fatal(e1.getLocalizedMessage());
			started = false;
		}

		if (!started)
			return;

		if (queryServer != updateServer) {
			try {
				queryServer = queryServerBoot.create();
				queryServer.start();
			} catch (IOException e) {
				logger.fatal(e.getLocalizedMessage());
				started = false;
			}
		}

		if (!started)
			return;

		if (authorizationServer != updateServer && authorizationServer != queryServer) {
			try {
				authorizationServer = authorizationServerBoot.create();
				authorizationServer.start();
			} catch (IOException e) {
				logger.fatal(e.getLocalizedMessage());
				started = false;
			}
		}

		if (!started)
			return;

		String host = "localhost";
		try {
			host = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.warn(e.getMessage());
		}

		System.out.println("Listening for SECURE SPARQL UPDATES on https://" + host + ":"
				+ properties.getSecureUpdatePort() + properties.getSecureUpdatePath());
		System.out.println("Listening for SECURE SPARQL QUERIES on https://" + host + ":"
				+ properties.getSecureQueryPort() + properties.getSecureQueryPath());
		System.out.println("Listening for REGISTRATION REQUESTS on https://" + host + ":"
				+ properties.getAuthorizationServerPort() + properties.getRegisterPath());
		System.out.println("Listening for TOKEN REQUESTS on https://" + host + ":"
				+ properties.getAuthorizationServerPort() + properties.getTokenRequestPath());
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