package it.unibo.arces.wot.sepa.engine.protocol.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;

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
 * </pre>
 * 
 *
 * 
 * @see QueryRequest
 * @see UpdateRequest
 */
public class QueryHandler extends SPARQL11Handler {

	public QueryHandler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, long timeout) throws IllegalArgumentException {
		super(request, exchange, context, scheduler, timeout);
	}

	@Override
	protected Request parse(HttpRequest request) {
		switch (httpRequest.getRequestLine().getMethod().toUpperCase()) {
		case "GET":
			logger.debug("query via GET");
			if (httpRequest.getRequestLine().getUri().contains("query=")) {
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "query is null");
				return null;
			}
			String[] query = httpRequest.getRequestLine().getUri().split("&");
			for (String param : query) {
				String[] value = param.split("=");
				if (value[0].equals("query")) {
					String sparql = "";
					try {
						sparql = URLDecoder.decode(value[1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
						return null;
					}
					Integer token = scheduler.getToken();
					if (token == -1) {
						Utilities.failureResponse(exchange, HttpStatus.SC_FORBIDDEN, "No more tokens");
						return null;
					}
					return new QueryRequest(token, sparql);
				}
			}
			Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Wrong format: " + httpRequest.getRequestLine());
			return null;

		case "POST":
			String body = null;
			HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
			try {
				body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			} catch (ParseException | IOException e) {
				body = e.getLocalizedMessage();
			}

			Header[] headers = httpRequest.getHeaders("Content-Type");
			if (headers.length != 1) {
				logger.error("Content-Type is missing");
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "Content-Type is missing");
				return null;
			}

			if (headers[0].getValue().equals("application/sparql-query")) {
				logger.debug("query via POST directly");
				Integer token = scheduler.getToken();
				if (token == -1) {
					Utilities.failureResponse(exchange, HttpStatus.SC_FORBIDDEN, "No more tokens");
					return null;
				}
				return new QueryRequest(token, body);
			} else if (headers[0].getValue().equals("application/x-www-form-urlencoded")) {
				String decodedBody;
				try {
					decodedBody = URLDecoder.decode(body, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, e.getMessage());
					return null;
				}

				String[] parameters = decodedBody.split("&");
				for (String param : parameters) {
					String[] value = param.split("=");
					if (value[0].equals("query")) {
						logger.debug("query via URL-encoded");
						Integer token = scheduler.getToken();
						if (token == -1) {
							Utilities.failureResponse(exchange, HttpStatus.SC_FORBIDDEN, "No more tokens");
							return null;
						}
						return new QueryRequest(token, value[1]);
					}
				}
			}

			logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			Utilities.failureResponse(exchange, HttpStatus.SC_NOT_FOUND,
					"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
			return null;
		}

		logger.error("UNSUPPORTED METHOD: " + httpRequest.getRequestLine().getMethod().toUpperCase());
		Utilities.failureResponse(exchange, HttpStatus.SC_NOT_FOUND,
				"Unsupported method: " + httpRequest.getRequestLine().getMethod().toUpperCase());

		return null;
	}

}
