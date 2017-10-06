package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
//import java.util.Observable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;

import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.engine.protocol.http.HttpUtilities;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

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
	protected static final Logger logger = LogManager.getLogger("UpdateHandler");

	public UpdateHandler(Scheduler scheduler) throws IllegalArgumentException {
		super(scheduler);
	}

	@Override
	protected Request parse(HttpAsyncExchange exchange) {
		if (!exchange.getRequest().getRequestLine().getMethod().toUpperCase().equals("POST")) {
			logger.error("UNSUPPORTED METHOD: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());
			HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
					"Unsupported method: " + exchange.getRequest().getRequestLine().getMethod().toUpperCase());
			return null;
		}

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

		if (headers[0].getValue().equals("application/sparql-update")) {
			logger.debug("update via POST directly");
			
			return new UpdateRequest(body);
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

				if (value[0].equals("update")) {
					logger.debug("update via URL-encoded");
					
					return new UpdateRequest(value[1]);
				}
			}
		}

		logger.error("Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		HttpUtilities.sendFailureResponse(exchange, HttpStatus.SC_BAD_REQUEST,
				"Request MUST conform to SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)");
		return null;
	}
}
