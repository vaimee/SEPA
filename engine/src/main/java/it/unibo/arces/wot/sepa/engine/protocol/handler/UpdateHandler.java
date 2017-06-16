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

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.protocol.http.Utilities;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;

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
 * update via POST directly   |    POST       using-graph-uri (0 or more)       application/sparql-update           Unencoded SPARQL update request string
 *                                            using-named-graph-uri (0 or more)
 * </pre>
 * 
 */
public class UpdateHandler extends SPARQL11Handler {

	public UpdateHandler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, long timeout) throws IllegalArgumentException {
		super(request, exchange, context, scheduler, timeout);
	}

	@Override
	protected Request parse(HttpRequest request) {
		if (!httpRequest.getRequestLine().getMethod().toUpperCase().equals("POST")) {
			logger.error("UNSUPPORTED METHOD: " + httpRequest.getRequestLine().getMethod().toUpperCase());
			Utilities.failureResponse(exchange, 400,
					"Unsupported method: " + httpRequest.getRequestLine().getMethod().toUpperCase());

			return null;
		}

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

		if (headers[0].getValue().equals("application/sparql-update")) {
			logger.debug("update via POST directly");
			Integer token = scheduler.getToken();
			if (token == -1) {
				Utilities.failureResponse(exchange, HttpStatus.SC_BAD_REQUEST, "No more tokens");
				return null;
			}
			return new UpdateRequest(token, body);
		} else if (headers[0].getValue().equals("application/x-www-form-urlencoded")) {
			String decodedBody;
			try {
				decodedBody = URLDecoder.decode(body, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error(e.getMessage());
				Utilities.failureResponse(exchange, 400, e.getMessage());
				return null;
			}

			String[] parameters = decodedBody.split("&");
			for (String param : parameters) {
				String[] value = param.split("=");

				if (value[0].equals("update")) {
					logger.debug("update via URL-encoded");
					
					Integer token = scheduler.getToken();
					if (token == -1) {
						Utilities.failureResponse(exchange, HttpStatus.SC_FORBIDDEN, "No more tokens");
						return null;
					}
					return new UpdateRequest(token, value[1]);
				}
			}
		}

		logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		Utilities.failureResponse(exchange, 400,
				"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		return null;
	}
}
