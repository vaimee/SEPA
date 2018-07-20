package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.net.URLDecoder;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.HttpAsyncExchange;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUQRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

/**
 * This method parse the HTTP request according to
 * <a href="https://www.w3.org/TR/sparql11-protocol/"> SPARQL 1.1 Protocol</a>
 *
 * 
 * @see QueryRequest
 * @see UpdateRequest
 */
public class QueryHandler extends SPARQL11Handler {
	protected static final Logger logger = LogManager.getLogger();

	public QueryHandler(Scheduler scheduler) throws IllegalArgumentException {
		super(scheduler);
	}

	@Override
	protected InternalUQRequest parse(HttpAsyncExchange exchange) throws SPARQL11ProtocolException {
		switch (exchange.getRequest().getRequestLine().getMethod().toUpperCase()) {
		case "GET":
			 /* <pre>
			 *                               HTTP Method   Query String Parameters           Request Content Type                Request Message Body
			 *----------------------------------------------------------------------------------------------------------------------------------------
			 * query via GET              |   GET          query (exactly 1)                 None                                None
			 *                            |                default-graph-uri (0 or more)
			 *                            |                named-graph-uri (0 or more)
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
			 * */
			logger.debug("query via GET");
			try {
				String requestUri = exchange.getRequest().getRequestLine().getUri();

				if (requestUri.indexOf('?') == -1) {
					throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST,
							"Wrong request uri: ? not found in " + requestUri);
				}

				String queryParameters = requestUri.substring(requestUri.indexOf('?') + 1);
				Map<String, String> params = HttpUtilities.splitQuery(queryParameters);

				if (params.get("query") == null) {
					throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST,
							"Wrong request uri: 'query=' not found in " + queryParameters);
				}

				String sparql = URLDecoder.decode(params.get("query"), "UTF-8");
				String graphUri = params.get("default-graph-uri");
				String namedGraphUri = params.get("named-graph-uri");

				return new InternalQueryRequest(sparql, graphUri, namedGraphUri);
			} catch (Exception e) {
				throw new SPARQL11ProtocolException(HttpStatus.SC_BAD_REQUEST, e.getMessage());
			}
		case "POST":
			return parsePost(exchange,"query");
		}

		logger.error("UNSUPPORTED METHOD: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());
		throw new SPARQL11ProtocolException(HttpStatus.SC_NOT_FOUND,
				"Unsupported method: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());
	}
}
