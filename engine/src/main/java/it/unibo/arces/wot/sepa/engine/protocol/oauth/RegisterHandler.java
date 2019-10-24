/* HTTP handler for digital identities registration
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
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;

import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpUtilities;

/**
 * HTTP handler for digital identities registration. Once registered, a digital identity would use the "client_credentials" grant to get/refresh a token
 * */
public class RegisterHandler implements HttpAsyncRequestHandler<HttpRequest> {
	private static final Logger logger = LogManager.getLogger();

	public RegisterHandler() {

	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange exchange, HttpContext context)
			throws HttpException, IOException {
		logger.info(">> REGISTRATION");

		String name = null;

		// Parsing and validating request headers
		// Content-Type: application/json
		// Accept: application/json
		try {

			Header[] headers = exchange.getRequest().getHeaders("Content-Type");
			if (headers.length == 0) {
				logger.error("Content-Type is missing");
				HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "content_type_error","Content-Type is missing"));
				return;
			}
			if (headers.length > 1) {
				logger.error("Too many Content-Type headers");
				HttpUtilities.sendFailureResponse(exchange,new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "content_type_error", "Too many Content-Type headers"));
				return;
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Content-Type must be: application/json");
				HttpUtilities.sendFailureResponse(exchange,new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "content_type_error","Content-Type must be: application/json"));
				return;
			}

			headers = exchange.getRequest().getHeaders("Accept");
			if (headers.length == 0) {
				logger.error("Accept is missing");
				HttpUtilities.sendFailureResponse(exchange,new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Accept is missing"));
				return;
			}
			if (headers.length > 1) {
				logger.error("Too many Accept headers");
				HttpUtilities.sendFailureResponse(exchange,new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Too many Accept headers"));
				return;
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Accept must be: application/json");
				HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "accept_error","Accept must be: application/json"));
				return;
			}
		} catch (NullPointerException e) {
			logger.error(e.getMessage());
			HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "NullPointerException",e.getMessage()));
			return;
		}

		// Parsing and validating request body
		/*
		 * {"register", { "client_identity": "IDENTITY", "grant_types":
		 * ["client_credentials"] } }
		 */
		try {
			String jsonString = "";
			HttpEntity entity = ((HttpEntityEnclosingRequest) exchange.getRequest()).getEntity();
			try {
				jsonString = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			} catch (ParseException  e) {
				HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "ParseException",e.getMessage()));
				return;
			}
			JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

			// Client identity
			name = json.get("register").getAsJsonObject().get("client_identity").getAsString();

			// Client credentials
			if (!json.get("register").getAsJsonObject().get("grant_types").getAsJsonArray().contains(new JsonPrimitive("client_credentials"))) {
				logger.error("\"grant_types\" must contain \"client_credentials\"");
				HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST,"invalid_grant",
						"\"grant_types\" must contain \"client_credentials\""));
				return;
			}

		} catch (NullPointerException e) {
			logger.error(e.getMessage());
			HttpUtilities.sendFailureResponse(exchange,new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "NullPointerException",e.getMessage()));
			return;
		}

		// *****************************************
		// Register client and retrieve credentials
		// *****************************************
		Response cred = Dependability.register(name);

		if (cred.getClass().equals(ErrorResponse.class)) {
			ErrorResponse error = (ErrorResponse) cred;
			logger.warn(error.toString());

			HttpUtilities.sendFailureResponse(exchange, error);
			return;
		}

		HttpUtilities.sendResponse(exchange, HttpStatus.SC_CREATED, cred.toString());
	}
}