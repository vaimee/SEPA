package it.unibo.arces.wot.sepa.engine.protocol.handler;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class TokenRequestHandler extends SPARQL11Handler {
	private AuthorizationManager am;

	public TokenRequestHandler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, AuthorizationManager am, long timeout) throws IllegalArgumentException {
		super(request, exchange, context, scheduler, timeout);

		this.am = am;
	}

	@Override
	public void run() {
		logger.info(">> REQUEST TOKEN");

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
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Too many Accept headers");
		}
		if (!headers[0].getValue().equals("application/json")) {
			logger.error("Accept must be: application/json");
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Accept must be: application/json");
		}

		// Authorization header
		headers = httpRequest.getHeaders("Authorization");
		if (headers.length != 1) {
			logger.error("Authorization is missing or multiple");
			Utilities.failureResponse(exchange, 401, "Authorization is missing or multiple");
			return;
		}

		// Extract Basic64 authorization
		String basic = headers[0].getValue();

		if (!basic.startsWith("Basic ")) {
			logger.error("Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
			Utilities.failureResponse(exchange, 401,
					"Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
			return;
		}

		// *************
		// Get token
		// *************
		Response token = am.getToken(basic.split(" ")[1]);

		if (token.getClass().equals(ErrorResponse.class)) {
			ErrorResponse error = (ErrorResponse) token;
			logger.error(token.toString());
			Utilities.failureResponse(exchange, error.getErrorCode(), error.getErrorMessage());
		} else {
			Utilities.sendResponse(exchange, 201, token.toString());
		}
	}
}
