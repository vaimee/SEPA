package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map;

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

import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.dependability.CORSManager;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.timing.Timings;

public abstract class SPARQL11Handler implements HttpAsyncRequestHandler<HttpRequest>, SPARQL11HandlerMBean {
	private static final Logger logger = LogManager.getLogger();

	private Scheduler scheduler;

	protected HTTPHandlerBeans jmx = new HTTPHandlerBeans();

	public SPARQL11Handler(Scheduler scheduler) throws IllegalArgumentException {

		if (scheduler == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;

		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
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
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_UNAUTHORIZED, "CORS origin not allowed");
			return false;
		}

		if (CORSManager.isPreFlightRequest(exchange)) {
			HttpUtilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
			return false;
		}

		return true;
	}

	protected abstract InternalUQRequest parse(HttpAsyncExchange exchange);

	/**
	 * <a href="https://www.w3.org/TR/sparql11-protocol/"> SPARQL 1.1 Protocol</a>
	 *
	 * *
	 * 
	 * <pre>
	 *                               HTTP Method   Query String Parameters           Request Content Type                Request Message Body
	 *----------------------------------------------------------------------------------------------------------------------------------------
	 * update via URL-encoded POST|   POST         None                              application/x-www-form-urlencoded   URL-encoded, ampersand-separated query parameters.
	 *                            |                                                                                     update (exactly 1)
	 *                            |                                                                                     using-graph-uri (0 or more)
	 *                            |                                                                                     using-named-graph-uri (0 or more)
	 *----------------------------------------------------------------------------------------------------------------------------------------																													
	 * update via POST directly   |   POST        using-graph-uri (0 or more)        application/sparql-update           Unencoded SPARQL update request string
	 *                                            using-named-graph-uri (0 or more)
	 * ----------------------------------------------------------------------------------------------------------------------------------------												
	 * query via URL-encoded POST |   POST         None                              application/x-www-form-urlencoded   URL-encoded, ampersand-separated query parameters.
	 *                            |                                                                                     query (exactly 1)
	 *                            |                                                                                     default-graph-uri (0 or more)
	 *                            |                                                                                     named-graph-uri (0 or more)
	 *----------------------------------------------------------------------------------------------------------------------------------------																													
	 * query via POST directly    |   POST         default-graph-uri (0 or more)
	 *                            |                named-graph-uri (0 or more)       application/sparql-query            Unencoded SPARQL query string
	 * </pre>
	 * 
	 */
	protected InternalUQRequest parsePost(HttpAsyncExchange exchange, String type) {
		String contentTypePost = "application/sparql-query";
		String defGraph = "default-graph-uri";
		String namedGraph = "named-graph-uri";
		if (type.equals("update")) {
			contentTypePost = "application/sparql-update";
			defGraph = "using-graph-uri";
			namedGraph = "using-named-graph-uri";
		}
		
		String sparql = null;
		String default_graph_uri = null;
		String named_graph_uri = null;
		
		try {
			HttpEntity entity = ((HttpEntityEnclosingRequest) exchange.getRequest()).getEntity();
			String body = EntityUtils.toString(entity, Charset.forName("UTF-8"));

			Header[] headers = exchange.getRequest().getHeaders("Content-Type");
			if (headers.length != 1) {
				logger.error("Content-Type is missing");
				throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
			}

			if (headers[0].getValue().equals(contentTypePost)) {
				logger.trace(type + " via POST directly");

				String requestUri = exchange.getRequest().getRequestLine().getUri();
				String graphUri = null;
				String namedGraphUri = null;

				if (requestUri.indexOf('?') != -1) {
					String queryParameters = requestUri.substring(requestUri.indexOf('?') + 1);
					Map<String, String> params = HttpUtilities.splitQuery(queryParameters);
					graphUri = params.get(defGraph);
					namedGraphUri = params.get(namedGraph);
				}
				
				sparql = body;
				if (graphUri != null) default_graph_uri = URLDecoder.decode(graphUri,"UTF-8");
				if (namedGraphUri != null) named_graph_uri = URLDecoder.decode(namedGraphUri,"UTF-8");
			} else if (headers[0].getValue().equals("application/x-www-form-urlencoded")) {
				logger.trace(type + " via URL ENCODED POST");

				String decodedBody = URLDecoder.decode(body, "UTF-8");
				Map<String, String> params = HttpUtilities.splitQuery(decodedBody);

				sparql = params.get(type);
				default_graph_uri = params.get(defGraph);
				named_graph_uri = params.get(namedGraph);
			} else {
				logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
				throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST,
						"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			}

		} catch (ParseException | IOException e) {
			logger.error(e.getMessage());
			throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST, e.getMessage());
		}
		
		if (type.equals("query"))
			return new InternalQueryRequest(sparql, default_graph_uri, named_graph_uri);
		else
			return new InternalUpdateRequest(sparql, default_graph_uri, named_graph_uri);
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
		// CORS
		if (!corsHandling(httpExchange)) {
			jmx.corsFailed();
			return;
		}
		InternalUQRequest sepaRequest = null;

		try {
			// Parsing SPARQL 1.1 request and attach a token
			sepaRequest = parse(httpExchange);
		} catch (SPARQL11ProtocolException e) {
			logger.error("Parsing failed: " + httpExchange.getRequest());
			HttpUtilities.sendFailureResponse(httpExchange, e.getCode(), "Parsing failed: " + e.getBody());
			jmx.parsingFailed();
			return;
		}

		// Validate
		if (!validate(httpExchange.getRequest())) {
			logger.error("Validation failed SPARQL: " + sepaRequest.getSparql());
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_BAD_REQUEST,
					"Validation failed SPARQL: " + sepaRequest.getSparql());
			jmx.validatingFailed();
			return;
		}

		// Authorize
		if (!authorize(httpExchange.getRequest())) {
			logger.error("Authorization failed SPARQL: " + sepaRequest.getSparql());
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_UNAUTHORIZED,
					"Authorization failed SPARQL: " + sepaRequest.getSparql());
			jmx.authorizingFailed();
			return;
		}

		// Schedule request
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest,new SPARQL11ResponseHandler(httpExchange, jmx));
		if (req == null) {
			logger.error("Out of tokens");
			HttpUtilities.sendFailureResponse(httpExchange, HttpStatus.SC_NOT_ACCEPTABLE,
					"Too many pending requests");
			jmx.outOfTokens();
		}
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
