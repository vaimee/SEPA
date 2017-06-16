package it.unibo.arces.wot.sepa.engine.protocol.http;

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

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.protocol.handler.QueryHandler;
import it.unibo.arces.wot.sepa.engine.protocol.handler.UpdateHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.CORSManager;

public class HttpRequestHandler implements HttpAsyncRequestHandler<HttpRequest>,HttpRequestHandlerMBean {
	protected Logger logger = LogManager.getLogger("HttpHandler");

	protected SchedulerInterface scheduler;

	protected String updatePath;
	protected String queryPath;
	protected long timeout;
	protected long queryRequests;
	protected long updateRequests;

	protected String getMBeanName() {
		return "SEPA:type=HTTP";
	}
	
	public HttpRequestHandler(EngineProperties properties, SchedulerInterface scheduler)
			throws IllegalArgumentException {
		SEPABeans.registerMBean(getMBeanName(), this);

		if (properties == null) {
			logger.fatal("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}

		if (scheduler == null) {
			logger.fatal("Scheduler is null");
			throw new IllegalArgumentException("Scheduler is null");
		}

		this.scheduler = scheduler;

		updatePath = properties.getUpdatePath();
		queryPath = properties.getQueryPath();
		timeout = properties.getHttpTimeout();
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		// Buffer request content in memory for simplicity
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {

		if (!CORSManager.processCORSRequest(httpExchange)) {
			Utilities.failureResponse(httpExchange, HttpStatus.SC_UNAUTHORIZED, "CORS origin not allowed");
			return;
		}

		if (CORSManager.isPreFlightRequest(httpExchange)) {
			Utilities.sendResponse(httpExchange, 204, null);
			return;
		}

		processRequestUri(request, httpExchange, context);
	}

	protected void processRequestUri(HttpRequest request, HttpAsyncExchange httpExchange, HttpContext context) {
		logger.debug(request.getRequestLine().getUri());

		String requestUri = request.getRequestLine().getUri();

		if (requestUri.equals(updatePath)){
			updateRequests++;
			new UpdateHandler(request, httpExchange, context, scheduler, timeout).start();
		}
		else if (requestUri.equals(queryPath)){
			queryRequests++;
			new QueryHandler(request, httpExchange, context, scheduler, timeout).start();
		}
		else if (requestUri.equals("/echo"))
			Utilities.sendResponse(httpExchange, HttpStatus.SC_OK, Utilities.buildEchoResponse(request).toString());
		else
			Utilities.failureResponse(httpExchange, HttpStatus.SC_NOT_FOUND, request.getRequestLine().getUri());
	}

	@Override
	public long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(long t) {
		timeout = t;
	}

	@Override
	public String getQueryPath() {
		return queryPath;
	}

	@Override
	public String getUpdatePath() {
		return updatePath;
	}

	@Override
	public long getQueryRequests() {
		return queryRequests;
	}

	@Override
	public long getUpdateRequests() {
		return updateRequests;
	}
}
