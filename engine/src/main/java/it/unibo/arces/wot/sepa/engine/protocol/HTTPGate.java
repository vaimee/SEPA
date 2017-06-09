/* This class implements the W3C SPARQL 1.1 Protocol 
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

package it.unibo.arces.wot.sepa.engine.protocol;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;

import java.nio.charset.Charset;

//import com.sun.net.httpserver.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.beans.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.scheduling.RequestResponseHandler.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.security.CORSManager;

/**
 * This class implements the SPARQL 1.1 Protocol
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public class HTTPGate extends Thread implements HTTPGateMBean {

	/** The scheduler of the SEPA engine. */
	protected SchedulerInterface scheduler;

	/** The HTTP servers */
	protected static HttpServer updateServer = null;
	protected static HttpServer queryServer = null;

	/** The logger */
	protected Logger logger = LogManager.getLogger("HTTPGate");

	/** The HTTP timeout */
	private long timeout = 2000;

	/** The number of current transactions */
	protected long transactions = 0;

	/** The number of update transactions */
	private long updateTransactions = 0;

	/** The number of query transactions */
	private long queryTransactions = 0;

	/**
	 * The engine properties
	 * 
	 * @see EngineProperties
	 */
	protected EngineProperties properties;

	protected String mBeanName = "SEPA:type=HTTPGate";

	private int failedTransactions = 0;

	protected boolean started = true;

	/**
	 * Instantiates a new HTTP gate.
	 *
	 * @param properties
	 *            the properties
	 * @param scheduler
	 *            the scheduler
	 * 
	 * @see SchedulerInterface
	 * @see EngineProperties
	 */
	public HTTPGate(EngineProperties properties, SchedulerInterface scheduler) throws IllegalArgumentException {
		if (properties == null) {
			logger.fatal("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}

		if (scheduler == null) {
			logger.fatal("Scheduler is null");
			throw new IllegalArgumentException("Scheduler is null");
		}

		this.properties = properties;
		this.scheduler = scheduler;
		
		updateServer = ServerBootstrap.bootstrap().setListenerPort(properties.getUpdatePort())
				.registerHandler(properties.getUpdatePath(), new SPARQLHandler(properties.getUpdatePath()))
				.registerHandler("/echo", new EchoHandler()).create();

		if (properties.getQueryPort() != properties.getUpdatePort()) {
			queryServer = ServerBootstrap.bootstrap().setListenerPort(properties.getUpdatePort())
					.registerHandler(properties.getQueryPath(), new SPARQLHandler(properties.getQueryPath()))
					.registerHandler("/echo", new EchoHandler()).create();
		} else queryServer = updateServer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		if (!started) return;
		try {
			updateServer.wait();
			queryServer.wait();
		} catch (InterruptedException e) {
			logger.info(e.getMessage());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#start()
	 */
	@Override
	public void start() {
		this.setName("Starting...");

		SEPABeans.registerMBean(mBeanName, this);

		try {
			updateServer.start();

			if (queryServer != updateServer) queryServer.start();

		} catch (IOException e) {
			logger.fatal(e.getMessage());
			started = false;
		}

		if (started) {
			String host = "localhost";
			try {
				host = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e) {
				logger.warn(e.getMessage());
			}

			System.out.println("Listening for SPARQL UPDATES on https://" + host + ":" + properties.getUpdatePort()
					+ properties.getUpdatePath());
			System.out.println("Listening for SPARQL QUERIES on https://" + host + ":" + properties.getQueryPort()
					+ properties.getQueryPath());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#interrupt()
	 */
	@Override
	public void interrupt() {
		logger.info("Kill signal received...stopping HTTP servers...");
		super.interrupt();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see arces.unibo.SEPA.beans.HTTPGateMBean#getTransactions()
	 */
	@Override
	public long getTotalTransactions() {
		return this.transactions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see arces.unibo.SEPA.beans.HTTPGateMBean#getQueryTransactions()
	 */
	@Override
	public long getQueryTransactions() {
		return this.queryTransactions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see arces.unibo.SEPA.beans.HTTPGateMBean#getUpdateTransactions()
	 */
	@Override
	public long getUpdateTransactions() {
		return this.updateTransactions;
	}

	/**
	 * Failure response.
	 *
	 * @param exchange
	 *            the exchange
	 * @param httpResponseCode
	 *            the http response code
	 * @param responseBody
	 *            the response body
	 */
	protected void failureResponse(HttpAsyncExchange exchange, int httpResponseCode, String responseBody) {
		failedTransactions++;

		JsonObject json = buildEchoResponse(exchange.getRequest());

		json.add("body", new JsonPrimitive(responseBody));
		json.add("code", new JsonPrimitive(httpResponseCode));

		sendResponse(exchange, httpResponseCode, json.toString());
	}

	/**
	 * Builds the echo response.
	 *
	 * @param exchange
	 *            the exchange
	 * @return the json object
	 */

	private JsonObject buildEchoResponse(HttpRequest request) {
		JsonObject json = new JsonObject();

		json.add("method", new JsonPrimitive(request.getRequestLine().getMethod().toUpperCase()));
		json.add("protocol", new JsonPrimitive(request.getProtocolVersion().getProtocol()));

		JsonObject headers = new JsonObject();

		for (Header header : request.getAllHeaders()) {
			headers.add(header.getName(), new JsonPrimitive(header.getValue()));
		}
		json.add("headers", headers);

		String body = "";
		HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
		try {
			body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
		} catch (ParseException | IOException e) {
			body = e.getLocalizedMessage();
		}

		json.add("body", new JsonPrimitive(body));

		return json;
	}

	/**
	 * Send response.
	 *
	 * @param exchange
	 *            the exchange
	 * @param httpResponseCode
	 *            the http response code
	 * @param response
	 *            the response
	 */
	protected void sendResponse(HttpAsyncExchange exchange, int httpResponseCode, String response) {
		logger.info("<< HTTP response (" + httpResponseCode + ") " + response);

		exchange.getResponse().addHeader("Content-Type", "application/json");
		exchange.getResponse().setStatusCode(httpResponseCode);

		try {
			exchange.getResponse().setEntity(new NStringEntity(response));
		} catch (UnsupportedEncodingException e) {
			exchange.getResponse().setStatusCode(ErrorResponse.INTERNAL_SERVER_ERROR);
		}

		exchange.submitResponse(new BasicAsyncResponseProducer(exchange.getResponse()));
	}

	/**
	 * The Class EchoHandler.
	 */
	public class EchoHandler implements HttpAsyncRequestHandler<HttpRequest> { // HttpHandler
																				// {

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange exchange, HttpContext context)
				throws HttpException, IOException {
			JsonObject json = buildEchoResponse(request);

			if (!CORSManager.processCORSRequest(exchange)) {
				failureResponse(exchange, ErrorResponse.UNAUTHORIZED, "CORS origin not allowed");
				return;
			}

			if (CORSManager.isPreFlightRequest(exchange))
				sendResponse(exchange, 204, null);
			else
				sendResponse(exchange, 200, json.toString());

		}

		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest request, HttpContext context)
				throws HttpException, IOException {

			return null;
		}
	}

	/**
	 * The Class SPARQLHandler.
	 */
	public class SPARQLHandler implements HttpAsyncRequestHandler<HttpRequest> {

		public SPARQLHandler(String updatePath) {
			logger.debug("SPARQL handler created on " + updatePath);
		}

		@Override
		public void handle(HttpRequest request, HttpAsyncExchange exchange, HttpContext context)
				throws HttpException, IOException {
			logger.info(">> HTTP request");
			transactions += 1;
			new HTTPRequestProcessor(exchange).start();

		}

		@Override
		public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest arg0, HttpContext arg1)
				throws HttpException, IOException {

			return null;
		}

		/**
		 * The Class Running.
		 */
		class HTTPRequestProcessor extends Thread implements ResponseAndNotificationListener {

			/** The HTTP exchange. */
			private HttpAsyncExchange httpExchange;

			/** The response. */
			private Response response = null;

			/**
			 * Instantiates a new running.
			 *
			 * @param httpExchange
			 *            the http exchange
			 */
			public HTTPRequestProcessor(HttpAsyncExchange httpExchange) {
				this.httpExchange = httpExchange;
			}

			/**
			 * This method parse the HTTP request according to
			 * <a href="https://www.w3.org/TR/sparql11-protocol/"> SPARQL 1.1
			 * Protocol</a>
			 *
			 * *
			 * 
			 * <pre>
			 *                               HTTP Method   Query String Parameters           Request Content Type                Request Message Body
			 *----------------------------------------------------------------------------------------------------------------------------------------
			 * query via GET              |   GET          query (exactly 1)                 None                                None
			 *                            |                default-graph-uri (0 or more)
			 *                            |                named-graph-uri (0 or more)
			 *----------------------------------------------------------------------------------------------------------------------------------------												
			 * query via URL-encoded POST |   POST         None                              application/x-www-form-urlencoded   URL-encoded, ampersand-separated query parameters.
			 *                            |                                                                                     query (exactly 1)
			 *                            |                                                                                     default-graph-uri (0 or more)
			 *                            |                                                                                     named-graph-uri (0 or more)
			 *----------------------------------------------------------------------------------------------------------------------------------------																													
			 * query via POST directly    |   POST         default-graph-uri (0 or more)
			 *                            |                named-graph-uri (0 or more)       application/sparql-query            Unencoded SPARQL query string
			 *----------------------------------------------------------------------------------------------------------------------------------------
			 * update via URL-encoded POST|   POST         None                              application/x-www-form-urlencoded   URL-encoded, ampersand-separated query parameters.
			 *                            |                                                                                     update (exactly 1)
			 *                            |                                                                                     using-graph-uri (0 or more)
			 *                            |                                                                                     using-named-graph-uri (0 or more)
			 *----------------------------------------------------------------------------------------------------------------------------------------																													
			 * update via POST directly   |    POST       using-graph-uri (0 or more)       application/sparql-update           Unencoded SPARQL update request string
			 *                                            using-named-graph-uri (0 or more)
			 * </pre>
			 * 
			 * @param httpExchange
			 *            the HTTP exchange information
			 * @return the corresponding request (update or query), otherwise
			 *         null
			 * 
			 * @see QueryRequest
			 * @see UpdateRequest
			 */
			private Request parseSPARQL11(HttpAsyncExchange httpExchange) {
				switch (httpExchange.getRequest().getRequestLine().getMethod().toUpperCase()) {
				case "GET":
					logger.debug("query via GET");
					if (httpExchange.getRequest().getRequestLine().getUri().contains("query=")) {
						failureResponse(httpExchange, 400, "query is null");
						return null;
					}
					String[] query = httpExchange.getRequest().getRequestLine().getUri().split("&");
					for (String param : query) {
						String[] value = param.split("=");
						if (value[0].equals("query")) {
							queryTransactions++;
							String sparql = "";
							try {
								sparql = URLDecoder.decode(value[1], "UTF-8");
							} catch (UnsupportedEncodingException e) {
								failureResponse(httpExchange, 400, e.getMessage());
								return null;
							}
							Integer token = 0;
							if (scheduler != null)
								token = scheduler.getToken();
							if (token == -1) {
								failureResponse(httpExchange, ErrorResponse.FORBIDDEN, "No more tokens");
								return null;
							}
							return new QueryRequest(token, sparql);
						}
					}
					failureResponse(httpExchange, 400, "Wrong format: " + httpExchange.getRequest().getRequestLine());
					return null;

				case "POST":
					String body = null;
					HttpEntity entity = ((HttpEntityEnclosingRequest) httpExchange.getRequest()).getEntity();
					try {
						body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
					} catch (ParseException | IOException e) {
						body = e.getLocalizedMessage();
					}

					if (httpExchange.getRequest().getHeaders("Content-Type").length != 1) {
						logger.error("Content-Type is missing");
						failureResponse(httpExchange, ErrorResponse.BAD_REQUEST, "Content-Type is missing");
						return null;
					}

					if (httpExchange.getRequest().getHeaders("Content-Type")[0].equals("application/sparql-query")) {
						logger.debug("query via POST directly");
						queryTransactions++;

						Integer token = 0;
						if (scheduler != null)
							token = scheduler.getToken();
						if (token == -1) {
							failureResponse(httpExchange, ErrorResponse.FORBIDDEN, "No more tokens");
							return null;
						}
						return new QueryRequest(token, body);
					} else if (httpExchange.getRequest().getHeaders("Content-Type")[0]
							.equals("application/sparql-update")) {
						logger.debug("update via POST directly");
						updateTransactions++;

						Integer token = 0;
						if (scheduler != null)
							token = scheduler.getToken();
						if (token == -1) {
							failureResponse(httpExchange, ErrorResponse.FORBIDDEN, "No more tokens");
							return null;
						}
						return new UpdateRequest(token, body);
					} else if (httpExchange.getRequest().getHeaders("Content-Type")[0]
							.equals("application/x-www-form-urlencoded")) {
						String decodedBody;
						try {
							decodedBody = URLDecoder.decode(body, "UTF-8");
						} catch (UnsupportedEncodingException e) {
							logger.error(e.getMessage());
							failureResponse(httpExchange, 400, e.getMessage());
							return null;
						}

						String[] parameters = decodedBody.split("&");
						for (String param : parameters) {
							String[] value = param.split("=");
							if (value[0].equals("query")) {
								logger.debug("query via URL-encoded");
								queryTransactions++;

								Integer token = 0;
								if (scheduler != null)
									token = scheduler.getToken();
								if (token == -1) {
									failureResponse(httpExchange, ErrorResponse.FORBIDDEN, "No more tokens");
									return null;
								}
								return new QueryRequest(token, value[1]);
							}
							if (value[0].equals("update")) {
								logger.debug("update via URL-encoded");
								updateTransactions++;

								Integer token = 0;
								if (scheduler != null)
									token = scheduler.getToken();
								if (token == -1) {
									failureResponse(httpExchange, ErrorResponse.FORBIDDEN, "No more tokens");
									return null;
								}
								return new UpdateRequest(token, value[1]);
							}
						}
					}

					logger.error(
							"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
					failureResponse(httpExchange, 400,
							"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
					return null;
				}

				logger.error(
						"UNSUPPORTED METHOD: " + httpExchange.getRequest().getRequestLine().getMethod().toUpperCase());
				failureResponse(httpExchange, 400,
						"Unsupported method: " + httpExchange.getRequest().getRequestLine().getMethod().toUpperCase());

				return null;
			}

			/**
			 * Validate.
			 *
			 * @param request
			 *            the request
			 * @return true, if successful
			 */
			private boolean validate(Request request) {
				// TODO SPARQL validation to be implemented
				return true;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.Thread#run()
			 */
			public void run() {
				if (!CORSManager.processCORSRequest(httpExchange)) {
					failureResponse(httpExchange, ErrorResponse.UNAUTHORIZED, "CORS origin not allowed");
					return;
				}

				if (CORSManager.isPreFlightRequest(httpExchange)) {
					sendResponse(httpExchange, 204, null);
					return;
				}

				// Parsing SPARQL 1.1 request and attach a token
				Request request = parseSPARQL11(httpExchange);

				// Parsing failed
				if (request == null) {
					logger.warn("SPARQL 1.1 SE parsing failed");
					failureResponse(httpExchange, ErrorResponse.BAD_REQUEST, "SPARQL 1.1 SE parsing failed");
					return;
				}

				// Timestamp
				long startTime = System.nanoTime();

				// Validate
				if (!validate(request)) {
					logger.error("SPARQL 1.1 SE validation failed " + request.getSPARQL());
					failureResponse(httpExchange, 400, "SPARQL 1.1 validation failed " + request.getSPARQL());
					scheduler.releaseToken(request.getToken());
					return;
				}

				// Timestamp
				long validatedTime = System.nanoTime();

				// Add request
				if (scheduler != null)
					scheduler.addRequest(request, this);
				else {
					logger.error("Scheduler is null");
					failureResponse(httpExchange, 500, "Scheduler is null");
					scheduler.releaseToken(request.getToken());
					return;
				}

				// Waiting response
				logger.debug("Waiting response in " + timeout + " ms...");

				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
				}

				// Timestamp
				long processedTime = System.nanoTime();

				// Logging
				logger.debug("Request validated in " + (startTime - validatedTime) + " and processed in "
						+ (validatedTime - processedTime) + " ns");

				// Send HTTP response
				if (response == null)
					sendResponse(httpExchange, 408, "Timeout");
				else {
					// Check response status
					JsonObject json = new JsonParser().parse(response.toString()).getAsJsonObject();

					// Query response
					if (response.getClass().equals(QueryResponse.class))
						sendResponse(httpExchange, json.get("code").getAsInt(), json.get("body").toString());
					else
						sendResponse(httpExchange, json.get("code").getAsInt(), json.toString());
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
	public long getFailedTransactions() {
		return failedTransactions;
	}
}
