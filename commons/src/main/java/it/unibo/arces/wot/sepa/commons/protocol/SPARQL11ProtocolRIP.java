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
import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryResultsFormat;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.SPARQLPrimitive;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This class implements the SPARQL 1.1 Protocol
 */

public class SPARQL11ProtocolRIP {

	/** The Constant logger. */
	private static final Logger logger = LogManager.getLogger("SPARQL11Protocol");

	/** The m bean name. */
	protected static String mBeanName = "arces.unibo.SEPA.server:type=SPARQL11Protocol";

	/** The properties. */
	protected SPARQL11Properties properties;

//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see java.lang.Object#toString()
//	 */
//	public String toString() {
//		return properties.toString();
//	}

	/**
	 * Instantiates a new SPARQL 11 protocol.
	 *
	 * @param properties
	 *            the properties
	 * @throws IllegalArgumentException
	 *             the illegal argument exception
	 * 
	 * @see SPARQL11Properties
	 */
	public SPARQL11ProtocolRIP(SPARQL11Properties properties) throws IllegalArgumentException {

		if (properties == null) {
			logger.fatal("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}

		this.properties = properties;
	}

	/**
	 * Update.
	 *
	 * @param req
	 *            the UPDATE request
	 * @return an UpdateResponse or ErrorResponse
	 * 
	 * @see UpdateRequest
	 * @see UpdateResponse
	 * @see ErrorResponse
	 */
	public Response update(UpdateRequest req) {
		return SPARQLProtocolOperation(SPARQLPrimitive.UPDATE, req);
	}

	/**
	 * Query.
	 *
	 * @param req
	 *            the QUERY request
	 * @return a QueryResponse or ErrorResponse
	 * 
	 * @see QueryRequest
	 * @see QueryResponse
	 * @see ErrorResponse
	 */
	public Response query(QueryRequest req) {
		return SPARQLProtocolOperation(SPARQLPrimitive.QUERY, req);
	}

	/**
	 * Implements a generic SPARQL 1.1 protocol operation.
	 *
	 * <pre>
	 * SPARQL 1.1 Protocol (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * UPDATE 2.2 update operation The response to an update request indicates
	 * success or failure of the request via HTTP response status code.
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
	  * </pre>
	*

	@param op
	 *            the op*
	@param request
	 *            the request*@return
	the response**

	@see SPARQLPrimitive
	 * @see Request
	 * @see Response
	 */
	protected Response SPARQLProtocolOperation(SPARQLPrimitive op, Request request) {
		// String response = null;
		HTTPMethod method = null;
		QueryResultsFormat format = null;

		if (op.equals(SPARQLPrimitive.QUERY)) {
			method = properties.getQueryMethod();
			format = properties.getQueryResultsFormat();
		} else {
			method = properties.getUpdateMethod();
		}

		// HTTP request build
		HttpUriRequest httpRequest = buildRequest(op, method, format, request.getSPARQL());
		if (httpRequest == null)
			return new ErrorResponse(request.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
					"Error on building HTTP request URI");
		
		//Execute request and parse response
		final CloseableHttpClient httpclient = HttpClients.createDefault();
		CloseableHttpResponse httpResponse = null;
		JsonObject jsonResponse = new JsonObject();
		
		try {
			long timing = System.nanoTime();

			//Execute HTTP request
			httpResponse = httpclient.execute(httpRequest);

			timing = System.nanoTime() - timing;

			if (op.equals(SPARQLPrimitive.QUERY))
				logger.debug("QUERY_TIME" + timing / 1000000 + " ms");
			else
				logger.debug("UPDATE_TIME " + timing / 1000000 + " ms");

			// Status code
			int code = httpResponse.getStatusLine().getStatusCode();

			// Body
			String body = null;
			HttpEntity entity = httpResponse.getEntity();
			try {
				body = EntityUtils.toString(entity, Charset.forName("UTF-8"));
			} catch (ParseException | IOException e) {
				code = HttpStatus.SC_INTERNAL_SERVER_ERROR;
				body = e.getMessage();
			}
			EntityUtils.consume(entity);

			JsonObject jsonBody = null;
			try {
				jsonBody = new JsonParser().parse(body).getAsJsonObject();
			} catch (JsonParseException | IllegalStateException e) {
				jsonResponse.add("body", new JsonPrimitive(body));
			}

			if (jsonBody != null)
				jsonResponse.add("body", jsonBody);

			jsonResponse.add("code", new JsonPrimitive(code));
			
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			 return new ErrorResponse(request.getToken(),HttpStatus.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
		} finally {
			// The underlying HTTP connection is still held by the response object to allow the response content to be streamed directly from the
			// network socket. In order to ensure correct deallocation of system resources the user MUST call CloseableHttpResponse#close() from a finally
			// clause. Please note that if response content is not fully consumed the underlying connection cannot be safely re-used and will be shut down and
			// discarded by the connection manager.
			try {
				httpResponse.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(request.getToken(),HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
		}

		 //Parsing the response (could be SPARQL 1.1 service specific)
		 return parseEndpointResponse(request.getToken(), jsonResponse.toString(), op, format);
	}

	/**
	 * Builds the request according to the
	 * <a href="https://www.w3.org/TR/sparql11-protocol"> SPARQL 1.1 Protocol
	 * </a>
	 *
	 * <pre>
	A query request can be:
	- via GET 
	- via URL-encoded POST
	- via POST directly
	
	An update request can be:
	- via URL-encoded POST
	- via POST directly
	 * </pre>
	 * 
	 * @param op
	 *            the SPARQLPrimitive
	 * @param method
	 *            the HTTPMethod
	 * @param format
	 *            the QueryResultsFormat
	 * @param sparql
	 *            the SPARQL Query or Update
	 * @return the http uri request
	 * 
	 * @see SPARQLPrimitive
	 * @see HTTPMethod
	 * @see QueryResultsFormat
	 * @see HttpUriRequest
	 */
	protected HttpUriRequest buildRequest(SPARQLPrimitive op, HTTPMethod method, QueryResultsFormat format,
			String sparql) {
		
		URI uri = null;
		HttpUriRequest httpRequest = null;
		String query = null;
		String contentType = "";
		ByteArrayEntity body = null;
		String accept = "application/json";

		/*
		 * HTTP Method Query String Parameters Request Content Type Request
		 * Message Body
		 * ---------------------------------------------------------------------
		 * -------------------------------------------------------------------
		 * query via GET GET query (exactly 1) None None default-graph-uri (0 or
		 * more) named-graph-uri (0 or more)
		 * ---------------------------------------------------------------------
		 * -------------------------------------------------------------------
		 * query via URL-encoded POST POST None
		 * application/x-www-form-urlencoded URL-encoded, ampersand-separated
		 * query parameters. query (exactly 1) default-graph-uri (0 or more)
		 * named-graph-uri (0 or more)
		 * ---------------------------------------------------------------------
		 * -------------------------------------------------------------------
		 * query via POST directly POST default-graph-uri (0 or more)
		 * named-graph-uri (0 or more) application/sparql-query Unencoded SPARQL
		 * query string
		 */
		// QUERY
		if (op.equals(SPARQLPrimitive.QUERY)) {
			// Support only the JSON query response format
			// (https://www.w3.org/TR/sparql11-results-json/)
			if (!QueryResultsFormat.JSON.equals(format))
				return null;

			accept = "application/sparql-results+json";

			switch (method) {
			case GET:
				try {
					query = URLEncoder.encode("query=" + sparql, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}
				break;
			case URL_ENCODED_POST:
				contentType = "application/x-www-form-urlencoded";

				try {
					String encodedContent = URLEncoder.encode(sparql, "UTF-8");
					body = new ByteArrayEntity(("query=" + encodedContent).getBytes());
					body.setContentType(contentType);
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}

				break;
			case POST:
				contentType = "application/sparql-query";
				try {
					body = new ByteArrayEntity(sparql.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}
				break;
			}
		}

		/*
		 * HTTP Method Query String Parameters Request Content Type Request
		 * Message Body
		 * ---------------------------------------------------------------------
		 * -------------------------------------------------------------------
		 * update via URL-encoded POST POST None
		 * application/x-www-form-urlencoded URL-encoded, ampersand-separated
		 * query parameters. update (exactly 1) using-graph-uri (0 or more)
		 * using-named-graph-uri (0 or more)
		 * ---------------------------------------------------------------------
		 * -------------------------------------------------------------------
		 * update via POST directly POST using-graph-uri (0 or more)
		 * application/sparql-update Unencoded SPARQL update request string
		 * using-named-graph-uri (0 or more)
		 */
		// UPDATE
		if (op.equals(SPARQLPrimitive.UPDATE)) {

			switch (method) {
			case URL_ENCODED_POST:
				contentType = "application/x-www-form-urlencoded";

				accept = "text/plain";

				try {
					String encodedContent = URLEncoder.encode(sparql, "UTF-8");
					body = new ByteArrayEntity(("update=" + encodedContent).getBytes());
					body.setContentType(contentType);
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}
				break;
			case POST:
				contentType = "application/sparql-update";

				accept = "*/*";

				byte[] bytes = null;
				try {
					bytes = sparql.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return null;
				}
				body = new ByteArrayEntity(bytes);
				body.setContentType("UTF-8");
				break;
			default:
				return null;
			}
		}

		// Path MAY be different for query and update
		String path;
		if (op.equals(SPARQLPrimitive.QUERY)) {
			path = properties.getQueryPath();
		} else {
			path = properties.getUpdatePath();
		}
		try {
			uri = new URI("http", null, properties.getHost(), properties.getHttpPort(), path, query, null);
		} catch (URISyntaxException e) {
			logger.error("Error on creating request URI " + e.getMessage());
			return null;
		}

		// GET or POST
		if (method.equals(HTTPMethod.GET))
			httpRequest = new HttpGet(uri);
		else
			httpRequest = new HttpPost(uri);

		// Headers
		if (contentType != null)
			httpRequest.setHeader("Content-Type", contentType);
		if (accept != null)
			httpRequest.setHeader("Accept", accept);

		// Request body
		if (body != null)
			((HttpPost) httpRequest).setEntity(body);

		logger.debug("HTTP Request: " + httpRequest.toString() + " Body: " + body.toString() + " Query: " + query);
		return httpRequest;
	}

	/**
	 * Parses the endpoint response.
	 *
	 * @param token
	 *            the token of the corresponding request
	 * @param jsonResponse
	 *            the JSON response to be parsed in the form {"code":HTTP Status
	 *            code,"body": "response body"}.
	 * @param op
	 *            the SPARQLPrimitive of the corresponding request
	 * @param format
	 *            the QueryResultsFormat in case of a query request
	 * @return the UpdateResponse, QueryResponse or ErrorResponse
	 * 
	 * @see SPARQLPrimitive
	 * @see QueryResultsFormat
	 * @see Response
	 * @see UpdateResponse
	 * @see QueryResponse
	 * @see ErrorResponse
	 */

	protected Response parseEndpointResponse(int token, String jsonResponse, SPARQLPrimitive op,
			QueryResultsFormat format) {
		logger.debug("Parse endpoint response #" + token + " " + jsonResponse);

		JsonObject json = null;
		try {
			json = new JsonParser().parse(jsonResponse).getAsJsonObject();
		} catch (JsonParseException | IllegalStateException e) {
			logger.warn(e.getMessage());

			if (op.equals(SPARQLPrimitive.UPDATE))
				return new UpdateResponse(token, jsonResponse);

			return new ErrorResponse(token, HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		if (json.get("code") != null) {
			if (json.get("code").getAsInt() >= 400)
				return new ErrorResponse(token, json.get("code").getAsInt(), json.get("body").getAsString());
		}

		if (op.equals(SPARQLPrimitive.UPDATE))
			return new UpdateResponse(token, json.get("body").getAsString());
		if (op.equals(SPARQLPrimitive.QUERY))
			return new QueryResponse(token, json.get("body").getAsJsonObject());

		return new ErrorResponse(token, HttpStatus.SC_INTERNAL_SERVER_ERROR, jsonResponse.toString());
	}
}
