package it.unibo.arces.wot.sepa.engine.protocol.handler;

import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;

import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;
import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;

public abstract class SPARQL11Handler extends Thread implements ResponseAndNotificationListener {
	protected Logger logger = LogManager.getLogger("SPARQL11SERequestProcessor");

	protected SchedulerInterface scheduler;
	protected long timeout;

	/** The HTTP exchange. */
	protected HttpAsyncExchange exchange;
	protected HttpRequest httpRequest;

	/** The response. */
	protected Response response = null;

	/**
	 * Instantiates a new running.
	 *
	 * @param  exchange
	 *            the http exchange
	 */
	public SPARQL11Handler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, long timeout) throws IllegalArgumentException {

		this.exchange = exchange;
		this.httpRequest = request;
		this.scheduler = scheduler;
		this.timeout = timeout;

		if (exchange == null)
			throw new IllegalArgumentException();
		if (request == null)
			throw new IllegalArgumentException();
		if (scheduler == null)
			throw new IllegalArgumentException();
	}

	protected boolean validate(HttpRequest request) {
		// TODO Validate SPARQL 1.1 Query
		return true;
	}

	protected boolean authorize(HttpRequest request) {
		// Always authorized
		return true;
	}

	protected Request parse(HttpRequest request) {
		// Not implemented as default
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		// Timestamp
		long startTime = System.nanoTime();

		// Parsing SPARQL 1.1 request and attach a token
		Request request = parse(httpRequest);

		// Timestamp
		long parsingTime = System.nanoTime();

		// Parsing failed
		if (request == null) {
			logger.error("Parsing failed: " + httpRequest);
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Parsing failed: " + httpRequest);
			return;
		}

		// Validate
		if (!validate(httpRequest)) {
			logger.error("Validation failed SPARQL: " + request.getSPARQL());
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
					"Validation failed SPARQL: " + request.getSPARQL());
			scheduler.releaseToken(request.getToken());
			return;
		}

		// Timestamp
		long validateTime = System.nanoTime();

		// Authorize
		if (!authorize(this.httpRequest)) {
			logger.error("Authorization failed SPARQL: " + request.getSPARQL());
			Utilities.failureResponse(exchange, HttpStatus.SC_UNAUTHORIZED,
					"Authorization failed SPARQL: " + request.getSPARQL());
			scheduler.releaseToken(request.getToken());
			return;
		}

		// Timestamp
		long authorizationTime = System.nanoTime();

		// Add request
		scheduler.addRequest(request, this);

		// Waiting response
		logger.debug("Waiting response in " + timeout + " ms...");

		try {
			Thread.sleep(timeout);
		} catch (InterruptedException e) {
		}

		// Timestamp
		long processedTime = System.nanoTime();

		float processing = ((float) (processedTime - startTime)) / 1000000;
		float parsing = ((float) (parsingTime - startTime)) / 1000000;
		float validating = ((float) (validateTime - parsingTime)) / 1000000;
		float authorizing = ((float) (authorizationTime - validateTime)) / 1000000;

		// Logging
		logger.info("Timing [Total:" + String.format("%.3f", processing) + ",Parsing:" + String.format("%.3f", parsing)
				+ ",Validating:" + String.format("%.3f", validating) + ",Authorizing:"
				+ String.format("%.3f", authorizing) + "] (ms)");

		// Send HTTP response
		if (response == null)
			Utilities.sendResponse(exchange, HttpStatus.SC_REQUEST_TIMEOUT, "Timeout");
		else {
			// Check response status
			JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();

			// Query response
			if (response.getClass().equals(QueryResponse.class))
				Utilities.sendResponse(exchange, json.get("code").getAsInt(), json.get("body").toString());
			else
				Utilities.sendResponse(exchange, json.get("code").getAsInt(), json.toString());
		}

		scheduler.releaseToken(request.getToken());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see arces.unibo.SEPA.scheduling.RequestResponseHandler.
	 * ResponseAndNotificationListener#notify(arces.unibo.SEPA.commons.
	 * response.Response)
	 */
	@Override
	public void notify(Response response) {
		logger.debug("Response #" + response.getToken());
		this.response = response;
		interrupt();
	}
}
