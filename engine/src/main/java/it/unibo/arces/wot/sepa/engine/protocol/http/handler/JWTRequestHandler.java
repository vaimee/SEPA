package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

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

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;

public class JWTRequestHandler implements HttpAsyncRequestHandler<HttpRequest> {
	protected static final Logger logger = LogManager.getLogger();

	private AuthorizationManager am;

	public JWTRequestHandler(AuthorizationManager am) throws IllegalArgumentException {
		if (am == null)
			throw new IllegalArgumentException();
		this.am = am;
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		logger.debug(">> REQUEST TOKEN");

		Header[] headers;
		// Parsing and validating request headers
		// Content-Type: application/json
		// Accept: application/json
		headers = request.getHeaders("Content-Type");
		if (headers.length == 0) {
			logger.error("Content-Type is missing");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
			return;
		}
		if (headers.length > 1) {
			logger.error("Too many Content-Type headers");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Too many Content-Type headers");
			return;
		}
		if (!headers[0].getValue().equals("application/json")) {
			logger.error("Content-Type must be: application/json");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST,
					"Content-Type must be: application/json");
			return;
		}

		headers = request.getHeaders("Accept");
		if (headers.length == 0) {
			logger.error("Accept is missing");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Accept is missing");
			return;
		}
		if (headers.length > 1) {
			logger.error("Too many Accept headers");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Too many Accept headers");
			return;
		}
		if (!headers[0].getValue().equals("application/json")) {
			logger.error("Accept must be: application/json");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST, "Accept must be: application/json");
			return;
		}

		// Authorization header
		headers = request.getHeaders("Authorization");
		if (headers.length != 1) {
			logger.error("Authorization is missing or multiple");
			HttpUtilities.sendFailureResponse(httpExchange, 401, "Authorization is missing or multiple");
			return;
		}

		// Extract Basic64 authorization
		String basic = headers[0].getValue();

		if (!basic.startsWith("Basic ")) {
			logger.error("Authorization must be \"Basic Basic64(<client_id>:<client_secret>)\"");
			HttpUtilities.sendFailureResponse(httpExchange, 401,
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
			HttpUtilities.sendFailureResponse(httpExchange, error.getErrorCode(), error.getErrorMessage());
		} else {
			HttpUtilities.sendResponse(httpExchange, HttpStatus.SC_CREATED, token.toString());
		}	
	}
}
