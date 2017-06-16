package it.unibo.arces.wot.sepa.engine.protocol.handler;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class RegisterHandler extends SPARQL11Handler {
	private AuthorizationManager am;
	
	public RegisterHandler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, AuthorizationManager am, long timeout) throws IllegalArgumentException {
		super(request, exchange, context, scheduler, timeout);
		
		this.am = am;
		this.httpRequest = request;
		this.exchange = exchange;
	}

	@Override
	public void run() {
		logger.info(">> REGISTRATION");

		String name = null;

		try {
			Header[] headers;
			// Parsing and validating request headers
			// Content-Type: application/json
			// Accept: application/json
			headers = httpRequest.getHeaders("Content-Type");
			if (headers.length == 0) {
				logger.error("Content-Type is missing");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
			}
			if (headers.length > 1) {
				logger.error("Too many Content-Type headers");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Too many Content-Type headers");
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Content-Type must be: application/json");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Content-Type must be: application/json");
			}

			headers = httpRequest.getHeaders("Accept");
			if (headers.length == 0) {
				logger.error("Accept is missing");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Accept is missing");
			}
			if (headers.length > 1) {
				logger.error("Too many Accept headers");
				Utilities.failureResponse(exchange,HttpStatus.SC_BAD_REQUEST, "Too many Accept headers");
			}
			if (!headers[0].getValue().equals("application/json")) {
				logger.error("Accept must be: application/json");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Accept must be: application/json");
			}

			// Parsing and validating request body
			/*
			 * { "client_identity": "IDENTITY", "grant_types":
			 * ["client_credentials"] }
			 */
			String jsonString = "";
			HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
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
				Utilities.failureResponse(exchange, 400, "\"grant_types\" must contain \"client_credentials\"");
				return;
			}
			name = json.get("client_identity").getAsString();
		} catch (NullPointerException e) {
			logger.error(e.getMessage());
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
			return;
		}

		// *****************************************
		// Register client and retrieve credentials
		// *****************************************
		Response cred = am.register(name);

		if (cred.getClass().equals(ErrorResponse.class)) {
			ErrorResponse error = (ErrorResponse) cred;
			logger.error(error.toString());
			
			Utilities.failureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
			return;
		}

		Utilities.sendResponse(exchange, 201, cred.toString());
	}
}