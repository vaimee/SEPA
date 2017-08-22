package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.IOException;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;

import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

import it.unibo.arces.wot.sepa.engine.security.CORSManager;

public abstract class SPARQL11Handler
		implements HttpAsyncRequestHandler<HttpRequest>, ResponseAndNotificationListener, SPARQL11HandlerMBean {
	private static final Logger logger = LogManager.getLogger("SPARQL11Handler");

	private Scheduler scheduler;

	protected HttpAsyncExchange httpExchange;

	protected HTTPHandlerBeans jmx = new HTTPHandlerBeans();

	public SPARQL11Handler(Scheduler scheduler, long timeout) throws IllegalArgumentException {

		if (scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;

		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		jmx.setTimeout(timeout);
	}

	protected boolean validate(HttpRequest request) {
		// TODO Validate SPARQL 1.1 Query
		return true;
	}

	protected boolean authorize(HttpRequest request) {
		// TODO Always authorized
		return true;
	}

	protected boolean corsHandling(HttpAsyncExchange exchange) {
		if (!CORSManager.processCORSRequest(exchange)) {
			Utilities.sendFailureResponse(exchange, HttpStatus.SC_UNAUTHORIZED,
					"CORS origin not allowed");
			return false;
		}

		if (CORSManager.isPreFlightRequest(exchange)) {
			Utilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
			return false;
		}

		return true;
	}

	protected abstract Request parse(HttpAsyncExchange exchange);

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		// Buffer request content in memory for simplicity
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {
		// Timestamps
		long startTime = System.nanoTime();

		long corsTime = -1;
		long parsingTime = -1;
		long validatingTime = -1;
		long authorizingTime = -1;
		long stopTime = -1;

		this.httpExchange = httpExchange;
		
		// CORS
		if (!corsHandling(httpExchange)) {
			jmx.updateTimings(startTime, System.nanoTime(), parsingTime, validatingTime, authorizingTime, stopTime);
			return;
		}
		corsTime = System.nanoTime();

		// Parsing SPARQL 1.1 request and attach a token
		Request sepaRequest = parse(httpExchange);
		parsingTime = System.nanoTime();

		// Parsing failed
		if (sepaRequest == null) {
			jmx.updateTimings(startTime, corsTime, parsingTime, validatingTime, authorizingTime, stopTime);

			logger.error("Parsing failed: " + request);
			Utilities.sendFailureResponse(httpExchange,HttpStatus.SC_BAD_REQUEST,
					"Parsing failed: " + request);
			return;
		}

		// Validate
		if (!validate(request)) {
			jmx.updateTimings(startTime, corsTime, parsingTime, System.nanoTime(), authorizingTime, stopTime);

			logger.error("Validation failed SPARQL: " + sepaRequest.getSPARQL());
			Utilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST,
					"Validation failed SPARQL: " + sepaRequest.getSPARQL());
			return;
		}
		validatingTime = System.nanoTime();

		// Authorize
		if (!authorize(request)) {
			jmx.updateTimings(startTime, corsTime, parsingTime, validatingTime, System.nanoTime(), stopTime);

			logger.error("Authorization failed SPARQL: " + sepaRequest.getSPARQL());
			Utilities.sendFailureResponse(httpExchange, HttpStatus.SC_UNAUTHORIZED,
					"Authorization failed SPARQL: " + sepaRequest.getSPARQL());
			return;
		}
		authorizingTime = System.nanoTime();

		// Schedule a new request
		int requestToken = scheduler.schedule(sepaRequest, this);

		if (requestToken == -1) {
			logger.warn("No more tokens");
			Utilities.sendFailureResponse(httpExchange, HttpStatus.SC_NOT_ACCEPTABLE,
					"No more tokens");
			return;
		}
		
		jmx.updateTimings(startTime, corsTime, parsingTime, validatingTime, authorizingTime, System.nanoTime());

		logger.info("Request #" + requestToken);
	}

	// Scheduler response
	@Override
	public void notify(Response response) {
		logger.info("Response #" + response.getToken());		

		JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();

		if (response.getClass().equals(QueryResponse.class))
			Utilities.sendResponse(httpExchange, json.get("code").getAsInt(), json.get("body").toString());
		else
			Utilities.sendResponse(httpExchange, json.get("code").getAsInt(), json.toString());
	}

	@Override
	public String getRequests() {
		return jmx.getRequests();
	}

	@Override
	public String getCORSTimings() {
		return jmx.getCORSTimings();
	}

	@Override
	public String getParsingTimings() {
		return jmx.getParsingTimings();
	}

	@Override
	public String getValidatingTimings() {
		return jmx.getValidatingTimings();
	}

	@Override
	public String getAuthorizingTimings() {
		return jmx.getAuthorizingTimings();
	}

	@Override
	public String getHandlingTimings() {
		return jmx.getHandlingTimings();
	}

	@Override
	public void setTimeout(long t) {
		jmx.setTimeout(t);

	}

	@Override
	public long getTimeout() {
		return jmx.getTimeout();
	}

	@Override
	public void reset() {
		jmx.reset();

	}
}
