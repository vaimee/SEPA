package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.bean.NgisLdHandlerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers.EntityById;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers.EntityList;
import it.unibo.arces.wot.sepa.engine.protocol.ngsild.handlers.ResourceHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class NgsiLdHandler implements HttpAsyncRequestHandler<HttpRequest>, NgsiLdHandlerMBean {
	private static final Logger logger = LogManager.getLogger();

	private ArrayList<ResourceHandler> handlers = new ArrayList<ResourceHandler>();

	protected NgisLdHandlerBeans jmx = new NgisLdHandlerBeans();

	public NgsiLdHandler(Scheduler scheduler) throws IllegalArgumentException {

		if (scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		// Add all resource handlers here
		handlers.add(new EntityList(scheduler, jmx));
		handlers.add(new EntityById(scheduler, jmx));
		
		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public void handleInternal(HttpRequest request, HttpResponse response, HttpContext context)
			throws HttpException, IOException {

		String method = request.getRequestLine().getMethod().toUpperCase();
		String requestURI = request.getRequestLine().getUri();
		String resourceURI =  requestURI.substring(new String("/ngsi-ld/v1").length());
		logger.debug("Request URI: "+requestURI+" Resource URI: "+resourceURI+" Method: "+method);
		
		JsonObject body = null;

		// Link header
		String link = null;
		if (request.containsHeader("Link"))
			link = request.getFirstHeader("Link").getValue();

		// Preconditions
		String cType = request.getFirstHeader("Content-Type").getValue();
		boolean cLength = request.containsHeader("Content-Length");

		if (method.equals("POST") || method.equals("PATCH")) {
			HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			if (entity != null)
				body = new JsonParser().parse(EntityUtils.toString(entity, Charset.forName("UTF-8"))).getAsJsonObject();
			
			if (cType == null) {
				NgsiLdError error = NgsiLdError.InvalidRequest;
				error.setTitle("Content-Type header is missing");
				error.setDetail(request.toString());
				response.setStatusCode(error.getErrorCode());
				NStringEntity responseBody = new NStringEntity(error.toString(),ContentType.create("application/json", "UTF-8"));
				response.setEntity(responseBody);
				jmx.validatingFailed();
				return;
			}

			if (!cLength) {
				response.setStatusCode(NgsiLdError.LengthRequired.getErrorCode());
				jmx.validatingFailed();
				return;
			}

			if (!cType.equals("application/json") && !cType.equals("application/ld+json")) {
				if (!method.equals("PATCH") || !cType.equals("application/merge-patch+json")) {
					NgsiLdError error = NgsiLdError.InvalidRequest;
					error.setTitle("Content-Type header is missing");
					error.setDetail("Content-Type is " + cType + ". Allowed values are application/json, application/ld+json or application/merge-patch+json (PATCH only)");
					response.setStatusCode(error.getErrorCode());
					NStringEntity responseBody = new NStringEntity(error.toString(), ContentType.create("application/json", "UTF-8"));
					response.setEntity(responseBody);
					jmx.validatingFailed();
					return;
				}
			}

			try {
				Integer.parseInt(request.getFirstHeader("Content-Length").getValue());
			} catch (NumberFormatException e) {
				NgsiLdError error = NgsiLdError.InvalidRequest;
				error.setTitle("Missing Content-Length");
				error.setDetail("Content-Length header shall include the length of the input payload");
				response.setStatusCode(error.getErrorCode());
				NStringEntity responseBody = new NStringEntity(error.toString(),ContentType.create("application/json", "UTF-8"));
				response.setEntity(responseBody);
				jmx.validatingFailed();
				return;
			}
		}

		if (cType == null && method.equals("GET"))
			cType = "application/json";

		if (method.equals("GET")) {
			if (!cType.equals("application/json") && !cType.equals("application/ld+json") && !cType.equals("*/*")) {
				NgsiLdError error = NgsiLdError.InvalidRequest;
				error.setTitle("Invalid Content-Type");
				error.setDetail("Content-Type is " + cType + ". Allowed values are application/json, application/ld+json or application/merge-patch+json (PATCH only)");
				response.setStatusCode(error.getErrorCode());
				NStringEntity responseBody = new NStringEntity(error.toString(),ContentType.create("application/json", "UTF-8"));
				response.setEntity(responseBody);
				jmx.validatingFailed();
				return;
			}
		}

		// Processing the request
		for (ResourceHandler handler : handlers) {
			if (handler.matches(resourceURI)) {
				handler.processRequest(method, body, link);

				response.setStatusCode(handler.getStatusCode());
				if (handler.getResponseBody() != null && handler.getResponseContentType() != null) {
					NStringEntity responseBody = new NStringEntity(handler.getResponseBody().toString(),
							ContentType.create(handler.getResponseContentType(), "UTF-8"));
					response.setEntity(responseBody);
				}
				if (handler.getResponseHeaders() != null) response.setHeaders(handler.getResponseHeaders());
				return;
			}
		}

		NgsiLdError error = NgsiLdError.OperationNotSupported;
		error.setTitle("Request cannot be handled");
		error.setDetail("ResourceURI:" + resourceURI + " Method:" + method + " Body:" + body);
		response.setStatusCode(error.getErrorCode());
		
		response.setStatusCode(NgsiLdError.OperationNotSupported.getErrorCode());
		NStringEntity responseBody = new NStringEntity(error.toString(),ContentType.create("application/json", "UTF-8"));
		response.setEntity(responseBody);
	}

	@Override
	public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return new BasicAsyncRequestConsumer();
	}

	@Override
	public void handle(HttpRequest data, HttpAsyncExchange httpExchange, HttpContext context)
			throws HttpException, IOException {

		// CORS
		if (!corsHandling(httpExchange)) {
			jmx.corsFailed();
			return;
		}

		jmx.start(httpExchange);
		HttpResponse response = httpExchange.getResponse();
		handleInternal(data, response, context);
		httpExchange.submitResponse(new BasicAsyncResponseProducer(response));
		jmx.stop(httpExchange);
	}

	protected boolean corsHandling(HttpAsyncExchange exchange) {
		if (!Dependability.processCORSRequest(exchange)) {
			logger.error("CORS origin not allowed");
			HttpUtilities.sendFailureResponse(exchange,
					new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "cors_error", "CORS origin not allowed"));
			return false;
		}

		if (Dependability.isPreFlightRequest(exchange)) {
			logger.error("Preflight request");
			HttpUtilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
			return false;
		}

		return true;
	}

	@Override
	public long getRequests() {
		return jmx.getRequests();
	}

	@Override
	public void reset() {
		jmx.reset();

	}

	@Override
	public float getHandlingTime_ms() {
		return jmx.getHandlingTime();
	}

	@Override
	public float getHandlingMinTime_ms() {
		return jmx.getHandlingMinTime();
	}

	@Override
	public float getHandlingAvgTime_ms() {
		return jmx.getHandlingAvgTime();
	}

	@Override
	public float getHandlingMaxTime_ms() {
		return jmx.getHandlingMaxTime_ms();
	}

	@Override
	public long getErrors_Timeout() {
		return jmx.getErrors_Timeout();
	}

	@Override
	public long getErrors_CORSFailed() {
		return jmx.getErrors_CORSFailed();
	}

	@Override
	public long getErrors_ParsingFailed() {
		return jmx.getErrors_ParsingFailed();
	}

	@Override
	public long getErrors_ValidatingFailed() {
		return jmx.getErrors_ValidatingFailed();
	}

}
