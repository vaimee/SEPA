package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

/**
 * This method parse the HTTP request according to
 * <a href="https://www.w3.org/TR/sparql11-protocol/"> SPARQL 1.1 Protocol</a>
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
                                  using-named-graph-uri (0 or more)
 * 
 * 2.1.4 Specifying an RDF Dataset
 * 
 * A SPARQL query is executed against an RDF Dataset. The RDF Dataset for a query may be specified either via the default-graph-uri and named-graph-uri parameters in the 
 * SPARQL Protocol or in the SPARQL query string using the FROM and FROM NAMED keywords. 
 * 
 * If different RDF Datasets are specified in both the protocol request and the SPARQL query string, 
 * then the SPARQL service must execute the query using the RDF Dataset given in the protocol request.
 * 
 * Note that a service may reject a query with HTTP response code 400 if the service does not allow protocol clients to specify the RDF Dataset.
 * If an RDF Dataset is not specified in either the protocol request or the SPARQL query string, 
 * then implementations may execute the query against an implementation-defined default RDF dataset.
 * </pre>
 * 
 *
 * 
 * @see QueryRequest
 * @see UpdateRequest
 */
public class QueryHandler extends SPARQL11Handler {
	protected static final Logger logger = LogManager.getLogger("QueryHandler");
	
	public QueryHandler(Scheduler scheduler) throws IllegalArgumentException {
		super(scheduler);
	}

	@Override
	protected Request parse(HttpAsyncExchange exchange) {
		switch (exchange.getRequest().getRequestLine().getMethod().toUpperCase()) {
		case "GET":
			logger.debug("query via GET");
			String requestUri = exchange.getRequest().getRequestLine().getUri();
			if (requestUri.indexOf('?') == -1) {
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Wrong request uri: ? not found in "+requestUri);
				return null;	
			}
			
			String queryParameters = requestUri.substring(requestUri.indexOf('?') + 1);
			
			if (!queryParameters.contains("query=")) {
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Wrong request uri: 'query=' not found in "+queryParameters);
				return null;
			}
			String[] query = queryParameters.split("&");
			for (String param : query) {
				String[] value = param.split("=");
				if (value[0].equals("query")) {
					String sparql = "";
					try {
						sparql = URLDecoder.decode(value[1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
						return null;
					}
					
					return new QueryRequest(sparql);
				}
			}
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Wrong format: " + exchange.getRequest().getRequestLine());
			return null;

		case "POST":
			String body = null;
			HttpEntity entity = ((HttpEntityEnclosingRequest) exchange.getRequest()).getEntity();
			try {
				body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			} catch (ParseException | IOException e) {
				body = e.getLocalizedMessage();
			}

			Header[] headers = exchange.getRequest().getHeaders("Content-Type");
			if (headers.length != 1) {
				logger.error("Content-Type is missing");
				HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
				return null;
			}

			if (headers[0].getValue().equals("application/sparql-query")) {
				logger.debug("query via POST directly");
				
				return new QueryRequest(body);
			} else if (headers[0].getValue().equals("application/x-www-form-urlencoded")) {
				String decodedBody;
				try {
					decodedBody = URLDecoder.decode(body, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
					return null;
				}

				String[] parameters = decodedBody.split("&");
				for (String param : parameters) {
					String[] value = param.split("=");
					if (value[0].equals("query")) {
						logger.debug("query via URL-encoded");
						
						return new QueryRequest(value[1]);
					}
				}
			}

			logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_NOT_FOUND,
					"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			return null;
		}

		logger.error("UNSUPPORTED METHOD: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());
		HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_NOT_FOUND,
				"Unsupported method: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());

		return null;
	}
}
