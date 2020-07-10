/* HTTP handler for token requests
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

package it.unibo.arces.wot.sepa.engine.protocol.oauth;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpUtilities;

/**
 * The class implements the "client_credentials" OAuth 2.0 grant
 * */

public class JWTRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {
	protected static final Logger logger = LogManager.getLogger();

	public JWTRequestHandler() throws IllegalArgumentException {

	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		if(corsHandling(httpExchange)) {
			handleTokenRequest(request, httpExchange);
		}
	}

	protected boolean corsHandling(HttpAsyncExchange exchange) {
		if (!Dependability.processCORSRequest(exchange)) {
			logger.error("CORS origin not allowed");
			HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "cors_error","CORS origin not allowed"));
			return false;
		}

		if (Dependability.isPreFlightRequest(exchange)) {
			logger.warn("Preflight request");
			HttpUtilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
			return false;
		}

		return true;
	}
	
	private void handleTokenRequest(HttpRequest request, HttpAsyncExchange httpExchange) {
		logger.info(">> REQUEST TOKEN");

		Header[] headers;
		// Parsing and validating request headers
		// Content-Type: application/json
		// Accept: application/json
		headers = request.getHeaders("Content-Type");
		if (headers.length == 0) {
			logger.error("Content-Type is missing");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "content_type_error","Content-Type is missing"));
			return;
		}
		if (headers.length > 1) {
			logger.error("Too many Content-Type headers");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "content_type_error","Too many Content-Type headers"));
			return;
		}
		if (!headers[0].getValue().equals("application/json")) {
			logger.error("Content-Type must be: application/json");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST,"content_type_error",
					"Content-Type must be: application/json"));
			return;
		}

		headers = request.getHeaders("Accept");
		if (headers.length == 0) {
			logger.error("Accept is missing");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Accept is missing"));
			return;
		}
		if (headers.length > 1) {
			logger.error("Too many Accept headers");
			HttpUtilities.sendFailureResponse(httpExchange,new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Too many Accept headers"));
			return;
		}
		if (!headers[0].getValue().equals("application/json")) {
			logger.error("Accept must be: application/json");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Accept must be: application/json"));
			return;
		}

		// Authorization header
		headers = request.getHeaders("Authorization");
		if (headers.length != 1) {
			logger.error("Authorization is missing or multiple");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"unauthorized_client", "Authorization is missing or multiple"));
			return;
		}

		// Extract Basic64 authorization
		String basic = headers[0].getValue();

		if (!basic.startsWith("Basic ")) {
			logger.error("Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
			HttpUtilities.sendFailureResponse(httpExchange,  new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"unauthorized_client","Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\""));
			return;
		}

		// *************
		// Get token
		// *************
		Response token = null;
		try {
			token = Dependability.getToken(basic.split(" ")[1]);
		} catch (SEPASecurityException e) {
			logger.error(e.getMessage());
			if (logger.isTraceEnabled()) e.printStackTrace();
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR,"dependability_not_configured", e.getMessage()));
			return;
		}

		if (token.getClass().equals(ErrorResponse.class)) {
			ErrorResponse error = (ErrorResponse) token;
			logger.error(token.toString());
			HttpUtilities.sendFailureResponse(httpExchange, error);
		} else {
			HttpUtilities.sendResponse(httpExchange, HttpStatus.SC_CREATED, token.toString());
		}
	}
}
