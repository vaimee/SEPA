/* HTTP handler for SPARQL 1.1 protocol
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

package it.unibo.arces.wot.sepa.engine.protocol.sparql11;

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

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.bean.HTTPHandlerBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.AuthorizationResponse;
import it.unibo.arces.wot.sepa.engine.dependability.authorization.Credentials;
import it.unibo.arces.wot.sepa.engine.gates.http.HttpUtilities;
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
			throw new IllegalArgumentException("Scheduler is null");

		this.scheduler = scheduler;

		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	protected boolean validate(String request) {
		// TODO Validate SPARQL 1.1
		return true;
	}

	protected abstract AuthorizationResponse authorize(HttpRequest request) throws SEPASecurityException;

	protected boolean corsHandling(HttpAsyncExchange exchange) {
		if (!Dependability.processCORSRequest(exchange)) {
			logger.error("CORS origin not allowed");
			HttpUtilities.sendFailureResponse(exchange, new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "cors_error","CORS origin not allowed"));
			return false;
		}

		if (Dependability.isPreFlightRequest(exchange)) {
			logger.warn("Preflight request");
			HttpUtilities.sendResponse(exchange, HttpStatus.SC_NO_CONTENT, "");
			return false;
		}

		return true;
	}

	protected abstract InternalUQRequest parse(HttpAsyncExchange exchange,Credentials credentials);

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
	protected InternalUQRequest parsePost(HttpAsyncExchange exchange, String type, Credentials credentials) {
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
			return new InternalQueryRequest(sparql, default_graph_uri, named_graph_uri,credentials);
		else
			return new InternalUpdateRequest(sparql, default_graph_uri, named_graph_uri,credentials);
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
		
		// Authorize
		AuthorizationResponse oauth = null;
		try {
			oauth = authorize(httpExchange.getRequest());
		} catch (SEPASecurityException e1) {
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,"oauth_exception",e1.getMessage()));
			jmx.authorizingFailed();
			return;
		}
		if (!oauth.isAuthorized()) {
//			final Header header = httpExchange.getRequest().getLastHeader("Authorization");
			logger.error("*** NOT AUTHORIZED *** " + oauth.getDescription());
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_UNAUTHORIZED,oauth.getError(),oauth.getDescription()));
			jmx.authorizingFailed();
			return;
		}
		
		InternalUQRequest sepaRequest = null;

		try {
			// Parsing SPARQL 1.1 request and attach a token
			sepaRequest = parse(httpExchange,oauth.getClientCredentials());
		} catch (SPARQL11ProtocolException e) {
			logger.error("Parsing failed: " + httpExchange.getRequest());
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "SPARQL11ProtocolException","Parsing failed: " + e.getBody()));
			jmx.parsingFailed();
			return;
		}

		// Validate SPARQL 1.1
		if (!validate(sepaRequest.getSparql())) {
			logger.error("Validation failed SPARQL: " + sepaRequest.getSparql());
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(HttpStatus.SC_BAD_REQUEST,"sparql_error",sepaRequest.getSparql()));
			jmx.validatingFailed();
			return;
		}

		// Schedule request
		Timings.log(sepaRequest);
		ScheduledRequest req = scheduler.schedule(sepaRequest,new SPARQL11ResponseHandler(httpExchange, jmx));
		if (req == null) {
			logger.error("Out of tokens");
			HttpUtilities.sendFailureResponse(httpExchange, new ErrorResponse(429,"too_many_requests","Too many pending requests"));
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
