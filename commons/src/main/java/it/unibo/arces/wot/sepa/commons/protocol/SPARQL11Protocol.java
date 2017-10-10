/* This class implements the SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)
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

package it.unibo.arces.wot.sepa.commons.protocol;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;

/**
 * This class implements the SPARQL 1.1 Protocol
 */

public class SPARQL11Protocol {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11Protocol");

	/** The m bean name. */
	protected static String mBeanName = "arces.unibo.SEPA.server:type=SPARQL11Protocol";

	/** The properties. */
	protected SPARQL11Properties properties;

	// HTTP fields
	final CloseableHttpClient httpClient = HttpClients.createDefault();
	final HttpPost updatePostRequest;
	final HttpPost queryPostRequest;
	HttpUriRequest queryRequest;
	
	public SPARQL11Protocol(SPARQL11Properties properties) throws IllegalArgumentException, URISyntaxException {
		if (properties == null) {
			logger.fatal("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}
		this.properties = properties;

		// Create update POST request
		updatePostRequest = new HttpPost(new URI("http", null, properties.getHost(), properties.getHttpPort(),
				properties.getUpdatePath(), null, null));
		updatePostRequest.setHeader("Accept", properties.getUpdateAcceptHeader());
		updatePostRequest.setHeader("Content-Type", properties.getUpdateContentTypeHeader());

		// Create query POST request
		if (!properties.getQueryMethod().equals(HTTPMethod.GET)) {
			queryPostRequest = new HttpPost(new URI("http", null, properties.getHost(), properties.getHttpPort(),
					properties.getQueryPath(), null, null));
			queryPostRequest.setHeader("Content-Type", properties.getQueryContentTypeHeader());
			queryPostRequest.setHeader("Accept", properties.getQueryAcceptHeader());
		} else
			queryPostRequest = null;
	}

	/**
	 * Implements a SPARQL 1.1 update operation
	 * (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * <pre>
	 * update via URL-encoded POST 
	 * - HTTP Method: POST
	 * - Query String Parameters: None
	 * - Request Content Type: <b>application/x-www-form-urlencoded</b>
	 * - Request Message Body: URL-encoded, ampersand-separated query parameters. <b>update</b> (exactly 1). using-graph-uri (0 or more). using-named-graph-uri (0 or more)
	 * 
	 * update via POST directly
	 * - HTTP Method: POST
	 * - Query String parameters: using-graph-uri (0 or more); using-named-graph-uri (0 or more)
	 * - Request Content Type: <b>application/sparql-update</b>
	 * - Request Message Body: Unencoded SPARQL update request string
	 * </pre>
	 * 
	 * UPDATE 2.2 update operation The response to an update request indicates
	 * success or failure of the request via HTTP response status code.
	 */
	public Response update(UpdateRequest req, int timeout) {
		StringEntity requestEntity = null;

		CloseableHttpResponse httpResponse = null;
		HttpEntity responseEntity = null;
		int responseCode = 0;
		String responseBody = null;

		try {
			// Set request entity
			if (properties.getUpdateMethod().equals(HTTPMethod.URL_ENCODED_POST)) {
				requestEntity = new StringEntity("update=" + req.getSPARQL(), Consts.UTF_8);
			} else if (properties.getUpdateMethod().equals(HTTPMethod.POST)) {
				requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
			}
			updatePostRequest.setEntity(requestEntity);
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
					.build();
			updatePostRequest.setConfig(requestConfig);

			// Execute HTTP request
			long timing = System.nanoTime();
			httpResponse = httpClient.execute(updatePostRequest);
			// httpResponse =
			// HttpClients.createDefault().execute(updateRequest);
			timing = System.nanoTime() - timing;
			logger.debug("ENDPOINT UPDATE_TIME (" + timing / 1000000 + " ms)");

			// Status code
			responseCode = httpResponse.getStatusLine().getStatusCode();

			// Body
			responseEntity = httpResponse.getEntity();
			responseBody = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
			EntityUtils.consume(responseEntity);

		} catch (IOException e) {
			return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			try {
				if (httpResponse != null)
					httpResponse.close();
			} catch (IOException e) {
				return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}

			requestEntity = null;
			responseEntity = null;
		}

		if (responseCode >= 400) {
			try {
				return new ErrorResponse(req.getToken(), new JsonParser().parse(responseBody).getAsJsonObject());
			} catch (JsonParseException e) {
				return new ErrorResponse(req.getToken(), responseCode, responseBody);
			}
		}

		return new UpdateResponse(req.getToken(), responseBody);
	}

	/**
	 * Implements a SPARQL 1.1 query operation
	 * (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * <pre>
	 * query via GET
	 * - HTTP Method: GET
	 * - Query String Parameters: <b>query</b> (exactly 1). default-graph-uri (0 or more). named-graph-uri (0 or more)
	 * - Request Content Type: None
	 * - Request Message Body: None
	 * 
	 * query via URL-encoded POST 
	 * - HTTP Method: POST
	 * - Query String Parameters: None
	 * - Request Content Type: <b>application/x-www-form-urlencoded</b>
	 * - Request Message Body: URL-encoded, ampersand-separated query parameters. <b>query</b> (exactly 1). default-graph-uri (0 or more). named-graph-uri (0 or more)
	 * 
	 * query via POST directly
	 * - HTTP Method: POST
	 * - Query String parameters: default-graph-uri (0 or more). named-graph-uri (0 or more)
	 * - Request Content Type: <b>application/sparql-query</b>
	 * - Request Message Body: Unencoded SPARQL update request string
	 *
	 * QUERY 2.1.5 Accepted Response Formats
	 * 
	 * Protocol clients should use HTTP content negotiation [RFC2616] to request
	 * response formats that the client can consume. See below for more on
	 * potential response formats.
	 * 
	 * 2.1.6 Success Responses
	 * 
	 * The SPARQL Protocol uses the response status codes defined in HTTP to
	 * indicate the success or failure of an operation. Consult the HTTP
	 * specification [RFC2616] for detailed definitions of each status code.
	 * While a protocol service should use a 2XX HTTP response code for a
	 * successful query, it may choose instead to use a 3XX response code as per
	 * HTTP.
	 * 
	 * The response body of a successful query operation with a 2XX response is
	 * either:
	 * 
	 * a SPARQL Results Document in XML, JSON, or CSV/TSV format (for SPARQL
	 * Query forms SELECT and ASK); or, an RDF graph [RDF-CONCEPTS] serialized,
	 * for example, in the RDF/XML syntax [RDF-XML], or an equivalent RDF graph
	 * serialization, for SPARQL Query forms DESCRIBE and CONSTRUCT). The
	 * content type of the response to a successful query operation must be the
	 * media type defined for the format of the response body.
	 * 
	 * 2.1.7 Failure Responses
	 * 
	 * The HTTP response codes applicable to an unsuccessful query operation
	 * include:
	 * 
	 * 400 if the SPARQL query supplied in the request is not a legal sequence
	 * of characters in the language defined by the SPARQL grammar; or, 500 if
	 * the service fails to execute the query. SPARQL Protocol services may also
	 * return a 500 response code if they refuse to execute a query. This
	 * response does not indicate whether the server may or may not process a
	 * subsequent, identical request or requests. The response body of a failed
	 * query request is implementation defined. Implementations may use HTTP
	 * content negotiation to provide human-readable or machine-processable (or
	 * both) information about the failed query request.
	 * 
	 * A protocol service may use other 4XX or 5XX HTTP response codes for other
	 * failure conditions, as per HTTP.
	 *
	 * </pre>
	 */
	public Response query(QueryRequest req, int timeout) {
		StringEntity requestEntity = null;

		CloseableHttpResponse httpResponse = null;
		HttpEntity responseEntity = null;
		int responseCode = 0;
		String responseBody = null;

		long timing = 0;
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout)
				.setConnectTimeout(timeout).build();
		
		try {
			if (properties.getQueryMethod().equals(HTTPMethod.GET)) {
				String query = URLEncoder.encode("query=" + req.getSPARQL(), "UTF-8");
				//requestEntity = new StringEntity("query=" + req.getSPARQL(), Consts.UTF_8);
				HttpGet queryGetRequest;
				try {
					queryGetRequest = new HttpGet(new URI("http", null, properties.getHost(), properties.getHttpPort(),
							properties.getQueryPath(), query, null));
				} catch (URISyntaxException e) {
					return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
				queryGetRequest.setConfig(requestConfig);
				queryRequest = queryGetRequest;
				
			} else {
				// Set request entity
				if (properties.getQueryMethod().equals(HTTPMethod.URL_ENCODED_POST)) {
					requestEntity = new StringEntity("query=" + req.getSPARQL(), Consts.UTF_8);
				} else if (properties.getQueryMethod().equals(HTTPMethod.POST)) {
					requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
				}
				queryPostRequest.setEntity(requestEntity);			
				queryPostRequest.setConfig(requestConfig);
				queryRequest = queryPostRequest;
			}

			// Execute HTTP request
			timing = System.nanoTime();
			httpResponse = httpClient.execute(queryRequest);
			timing = System.nanoTime() - timing;
			logger.debug("ENDPOINT QUERY TIME (" + timing / 1000000 + " ms)");

			// Status code
			responseCode = httpResponse.getStatusLine().getStatusCode();

			// Body
			responseEntity = httpResponse.getEntity();
			responseBody = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
			EntityUtils.consume(responseEntity);

		} catch (IOException e) {
			return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			try {
				if (httpResponse != null)
					httpResponse.close();
			} catch (IOException e) {
				return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}

			requestEntity = null;
			responseEntity = null;
		}

		if (responseCode >= 400) {
			try {
				return new ErrorResponse(req.getToken(), new JsonParser().parse(responseBody).getAsJsonObject());
			} catch (JsonParseException e) {
				return new ErrorResponse(req.getToken(), responseCode, responseBody);
			}
		}

		return new QueryResponse(req.getToken(), new JsonParser().parse(responseBody).getAsJsonObject());
	}
}
