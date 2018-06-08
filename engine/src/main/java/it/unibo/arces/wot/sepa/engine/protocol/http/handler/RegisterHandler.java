package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

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
import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;

public class RegisterHandler implements HttpAsyncRequestHandler<HttpRequest> {
	private static final Logger logger = LogManager.getLogger();

	private AuthorizationManager am;

	public RegisterHandler(AuthorizationManager am) {
		this.am = am;
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange exchange, HttpContext context)
			throws HttpException, IOException {
		logger.debug(">> REGISTRATION");

		String name = null;

		// Parsing and validating request headers
		// Content-Type: application/json
		// Accept: application/json
		try {

			Header[] headers = exchange.getRequest().getHeaders("Content-Type");
			if (headers.length == 0) {
				logger.error("Content-Type is missing");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
				return;
			}
			if (headers.length > 1) {
				logger.error("Too many Content-Type headers");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Too many Content-Type headers");
				return;
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Content-Type must be: application/json");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
						"Content-Type must be: application/json");
				return;
			}

			headers = exchange.getRequest().getHeaders("Accept");
			if (headers.length == 0) {
				logger.error("Accept is missing");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Accept is missing");
				return;
			}
			if (headers.length > 1) {
				logger.error("Too many Accept headers");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Too many Accept headers");
				return;
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Accept must be: application/json");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
						"Accept must be: application/json");
				return;
			}
		} catch (NullPointerException e) {
			logger.error(e.getMessage());
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
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
			} catch (ParseException | IOException e) {
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getLocalizedMessage());
				return;
			}
			JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();

			// Client identity
			name = json.get("register").getAsJsonObject().get("client_identity").getAsString();

			// Client credentials
			if (!json.get("register").getAsJsonObject().get("grant_types").getAsJsonArray().contains(new JsonPrimitive("client_credentials"))) {
				logger.error("\"grant_types\" must contain \"client_credentials\"");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
						"\"grant_types\" must contain \"client_credentials\"");
				return;
			}

		} catch (NullPointerException e) {
			logger.error(e.getMessage());
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
			return;
		}

		// *****************************************
		// Register client and retrieve credentials
		// *****************************************
		Response cred = am.register(name);

		if (cred.getClass().equals(ErrorResponse.class)) {
			ErrorResponse error = (ErrorResponse) cred;
			logger.error(error.toString());

			HttpUtilities.sendFailureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
			return;
		}

		HttpUtilities.sendResponse(exchange, HttpStatus.SC_CREATED, cred.toString());
	}
}