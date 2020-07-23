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
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.net.ssl.SSLException;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.timing.Timings;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.logging.log4j.LogManager;

/**
 * This class implements the SPARQL 1.1 Protocol
 */

public class SPARQL11Protocol implements java.io.Closeable {

	/** The log4j2 logger. */
	private static final Logger logger = LogManager.getLogger();

	/** The Java bean name. */
	protected static String mBeanName = "arces.unibo.SEPA.server:type=SPARQL11Protocol";

	/** The http client. */
	protected CloseableHttpClient httpClient;

	/** The security manager */
	protected final ClientSecurityManager sm;

	public SPARQL11Protocol(ClientSecurityManager sm) throws SEPASecurityException {
		this.sm = sm;
		if (sm == null)
			httpClient = HttpClients.createDefault();
		// httpClient = HttpClients.custom().setRetryHandler(new
		// SPARQL11RetryHandler()).build();
		else
			httpClient = sm.getSSLHttpClient();
	}

	public SPARQL11Protocol() {
		this.sm = null;
		httpClient = HttpClients.createDefault();
	}

	private Response executeRequest(HttpUriRequest req, Request request) {
		CloseableHttpResponse httpResponse = null;
		HttpEntity responseEntity = null;
		int responseCode = 0;
		String responseBody = null;
		ErrorResponse errorResponse = null;

		// Add "Authorization" header if required
		String authorizationHeader = request.getAuthorizationHeader();
		if (authorizationHeader != null) {
			req.setHeader("Authorization", authorizationHeader);
		}

		try {
			// Execute HTTP request
			logger.trace(req.toString() + " " + request.toString() + " (timeout: " + request.getTimeout() + " ms) ");

			long start = Timings.getTime();

			httpResponse = httpClient.execute(req);

			long stop = Timings.getTime();

			if (request.getClass().equals(UpdateRequest.class))
				Timings.log("HTTP_UPDATE_TIME", start, stop);
			else
				Timings.log("HTTP_QUERY_TIME", start, stop);

			// Status code
			responseCode = httpResponse.getStatusLine().getStatusCode();

			// Body
			responseEntity = httpResponse.getEntity();
			responseBody = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
			logger.trace(String.format("Response code: %d", responseCode));
			EntityUtils.consume(responseEntity);

			/*
			 * http://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/fundamentals.
			 * html#d5e279
			 * 
			 * 1.5. Exception handling
			 * 
			 * HTTP protocol processors can throw two types of exceptions:
			 * 
			 * 1) java.io.IOException in case of an I/O failure such as socket timeout or an
			 * socket reset 2) HttpException that signals an HTTP failure such as a
			 * violation of the HTTP protocol.
			 * 
			 * Usually I/O errors are considered non-fatal and recoverable, whereas HTTP
			 * protocol errors are considered fatal and cannot be automatically recovered
			 * from. Please note that HttpClient implementations re-throw HttpExceptions as
			 * ClientProtocolException, which is a subclass of java.io.IOException. This
			 * enables the users of HttpClient to handle both I/O errors and protocol
			 * violations from a single catch clause.
			 */
			
		} catch (IOException e) {
			if (e instanceof InterruptedIOException) {
				if (e instanceof SocketTimeoutException)
					errorResponse = new ErrorResponse(HttpStatus.SC_REQUEST_TIMEOUT, "SocketTimeoutException",
							e.getMessage() + " [timeout: " + request.getTimeout()+" ms retry: "+request.getNRetry()+"]");
				else if (e instanceof RequestAbortedException)
					errorResponse = new ErrorResponse(HttpStatus.SC_REQUEST_TIMEOUT, "RequestAbortedException",
							e.getMessage() + " [timeout: " + request.getTimeout()+" ms retry: "+request.getNRetry()+"]");
				else {
					e.printStackTrace();
					errorResponse = new ErrorResponse(HttpStatus.SC_SERVICE_UNAVAILABLE, "InterruptedIOException",
							e.getMessage());
				}
			} else if (e instanceof UnknownHostException) {
				errorResponse = new ErrorResponse(HttpStatus.SC_NOT_FOUND, "UnknownHostException", e.getMessage());
			} else if (e instanceof ConnectTimeoutException) {
				errorResponse = new ErrorResponse(HttpStatus.SC_REQUEST_TIMEOUT, "ConnectTimeoutException",
						e.getMessage());
			} else if (e instanceof SSLException) {
				errorResponse = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "SSLException", e.getMessage());
			} else if (e instanceof ClientProtocolException) {
				errorResponse = new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, "ClientProtocolException",
						e.getMessage());
			} else {
				errorResponse = new ErrorResponse(HttpStatus.SC_NOT_FOUND, "IOException", e.getMessage());
			}
		} finally {
			try {
				if (httpResponse != null)
					httpResponse.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "IOException", e.getMessage());
			}

			responseEntity = null;
		}

		if (responseCode >= 400) {
			// SPARQL 1.1 protocol does not recommend any format, while SPARQL 1.1 SE
			// suggests to use a JSON format
			// http://mml.arces.unibo.it/TR/sparql11-se-protocol.html#ErrorResponses
			try {
				JsonObject ret = new JsonParser().parse(responseBody).getAsJsonObject();
				errorResponse = new ErrorResponse(ret.get("status_code").getAsInt(), ret.get("error").getAsString(),
						ret.get("error_description").getAsString());
			} catch (Exception e) {
				logger.error(e.getMessage() + " response code:" + responseCode + " response body: " + responseBody);
				if (responseBody.equals(""))
					responseBody = httpResponse.toString();
				errorResponse = new ErrorResponse(responseCode, "sparql11_endpoint", responseBody);
			}
		}

		// ERRORS: if timeout retry...
		if (errorResponse != null) {
			logger.error(errorResponse);
			
			if (errorResponse.isTimeout() && request.getNRetry() > 0) {
				logger.warn("*** TIMEOUT RETRY "+request.getNRetry()+" ***");
				request.retry();
				if (sm == null)
					httpClient = HttpClients.createDefault();
				else
					try {
						httpClient = sm.getSSLHttpClient();
					} catch (SEPASecurityException e) {
						return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "SEPASecurityException", e.getMessage()+" while retrying exec "+errorResponse.getErrorDescription());
					}
				return executeRequest(req, request);		
			}
			
			/*
Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: I/O exception (java.net.SocketException) caught when processing request to {}->http://localhost:8000: Bad file descriptor
Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: Retrying request to {}->http://localhost:8000

Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: I/O exception (java.net.SocketException) caught when processing request to {}->http://localhost:8000: Bad file descriptor
Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: Retrying request to {}->http://localhost:8000

Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: I/O exception (java.net.SocketException) caught when processing request to {}->http://localhost:8000: Bad file descriptor
Jul 22, 2020 10:29:05 AM org.apache.http.impl.execchain.RetryExec execute
INFORMAZIONI: Retrying request to {}->http://localhost:8000
			 * */
			if (errorResponse.isBadFileDescriptor() && request.getNRetry() > 0) {
				logger.warn("*** BAD FILE DESCRIPTOR RETRY "+request.getNRetry()+" ***");
				
				try {
					httpClient.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					logger.error(e1.getMessage());
					return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "BadFileDescriptor", errorResponse.getErrorDescription()+" "+e1.getMessage());
				}
				
				if (sm == null)
					httpClient = HttpClients.createDefault();
				else
					try {
						httpClient = sm.getSSLHttpClient();
					} catch (SEPASecurityException e) {
						logger.error(e.getMessage());
						return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "BadFileDescriptor", errorResponse.getErrorDescription()+" "+e.getMessage());
					}
							
				request.retry();
				return executeRequest(req, request);
			}
			
			return errorResponse;
		}

		return (request.getClass().equals(UpdateRequest.class) ? new UpdateResponse(responseBody)
				: new QueryResponse(responseBody));
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
	 * 2.1.4 Specifying an RDF Dataset
	 * 
	 * A SPARQL query is executed against an RDF Dataset. The RDF Dataset for a query may be specified either 
	 * via the default-graph-uri and named-graph-uri parameters in the SPARQL Protocol or in the SPARQL query 
	 * string using the FROM and FROM NAMED keywords. If different RDF Datasets are specified in both the protocol 
	 * request and the SPARQL query string, then the SPARQL service must execute the query using the RDF Dataset 
	 * given in the protocol request.
	 * 
	 * Note that a service may reject a query with HTTP response code 400 if the service does not allow protocol 
	 * clients to specify the RDF Dataset. If an RDF Dataset is not specified in either the protocol request or 
	 * the SPARQL query string, then implementations may execute the query against an implementation-defined default RDF dataset.
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
	public Response query(QueryRequest req) {
		switch (req.getHttpMethod()) {
		case GET:
			return get(req);
		case POST:
			return post(req);
		case URL_ENCODED_POST:
			return post(req);
		}
		return post(req);
	}

	/**
	 * Implements a SPARQL 1.1 update operation
	 * (https://www.w3.org/TR/sparql11-protocol/)
	 * 
	 * <pre>
	 * update via URL-encoded POST 
	 * - Method: <b>POST</b>
	 * - Query Parameters: None
	 * - Content Type: <b>application/x-www-form-urlencoded</b>
	 * - Body: URL-encoded, ampersand-separated query parameters. 
	 * 	<b>update</b> (exactly 1). 
	 * 	<b>using-graph-uri</b> (0 or more). 
	 * 	<b>using-named-graph-uri</b> (0 or more)
	 * 
	 * update via POST directly
	 * - Method: <b>POST</b>
	 * - Query Parameters: 
	 * 	<b>using-graph-uri</b> (0 or more); 
	 * 	<b>using-named-graph-uri</b> (0 or more)
	 * - Content Type: <b>application/sparql-update</b>
	 * - Body: Unencoded SPARQL update request string
	 * </pre>
	 * 
	 * 2.2.3 Specifying an RDF Dataset
	 * 
	 * <pre>
	 * SPARQL Update requests are executed against a Graph Store, a mutable
	 * container of RDF graphs managed by a SPARQL service. The WHERE clause of a
	 * SPARQL update DELETE/INSERT operation [UPDATE] matches against data in an RDF
	 * Dataset, which is a subset of the Graph Store. The RDF Dataset for an update
	 * operation may be specified either in the operation string itself using the
	 * USING, USING NAMED, and/or WITH keywords, or it may be specified via the
	 * using-graph-uri and using-named-graph-uri parameters.
	 * 
	 * It is an error to supply the using-graph-uri or using-named-graph-uri
	 * parameters when using this protocol to convey a SPARQL 1.1 Update request
	 * that contains an operation that uses the USING, USING NAMED, or WITH clause.
	 * 
	 * A SPARQL Update processor should treat each occurrence of the
	 * using-graph-uri=g parameter in an update protocol operation as if a USING <g>
	 * clause were included for every operation in the SPARQL 1.1 Update request.
	 * Similarly, a SPARQL Update processor should treat each occurrence of the
	 * using-named-graph-uri=g parameter in an update protocol operation as if a
	 * USING NAMED <g> clause were included for every operation in the SPARQL 1.1
	 * Update request.
	 * 
	 * UPDATE 2.2 update operation The response to an update request indicates
	 * success or failure of the request via HTTP response status code.
	 * </pre>
	 */
	public Response update(UpdateRequest req) {
		switch (req.getHttpMethod()) {
		case POST:
			return post(req);
		case URL_ENCODED_POST:
			return post(req);
		default:
			return new ErrorResponse(HttpStatus.SC_METHOD_NOT_ALLOWED, "unsupported_method",
					"SPARQL 1.1 Update supports POST method only");
		}
	}

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
	 */
	private Response post(UpdateRequest req) {
		StringEntity requestEntity = null;
		HttpPost post;
		String graphs = null;

		// Setting URL
		String scheme = req.getScheme();
		String host = req.getHost();
		int port = req.getPort();
		String updatePath = req.getPath();

		// Create POST request
		try {
			for (String g : req.getDefaultGraphUri()) {
				if (graphs == null)
					graphs = "using-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&using-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}
			for (String g : req.getNamedGraphUri()) {
				if (graphs == null)
					graphs = "using-named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&using-named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}

			if (req.getHttpMethod().equals(HTTPMethod.POST)) {
				post = new HttpPost(new URI(scheme, null, host, port, updatePath, graphs, null));
				post.setHeader("Content-Type", "application/sparql-update");

				// Body
				requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
			} else {
				post = new HttpPost(new URI(scheme, null, host, port, updatePath, null, null));
				post.setHeader("Content-Type", "application/x-www-form-urlencoded");

				// Body
				if (graphs != null)
					requestEntity = new StringEntity(
							"update=" + URLEncoder.encode(req.getSPARQL(), "UTF-8") + "&" + graphs, Consts.UTF_8);
				else
					requestEntity = new StringEntity("update=" + URLEncoder.encode(req.getSPARQL(), "UTF-8"),
							Consts.UTF_8);
			}
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "URISyntaxException", e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e.getMessage());
		}

		// Accept header
		post.setHeader("Accept", req.getAcceptHeader());

		// Body
		post.setEntity(requestEntity);

		// Setting timeout
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) req.getTimeout())
				.setConnectTimeout((int) req.getTimeout()).build();
		post.setConfig(requestConfig);

		return executeRequest(post, req);
	}

	/**
	 * <pre>
	 *                               HTTP Method   Query String Parameters           Request Content Type                Request Message Body
	 *----------------------------------------------------------------------------------------------------------------------------------------											
	 * query via URL-encoded POST |   POST         None                              application/x-www-form-urlencoded   URL-encoded, ampersand-separated query parameters.
	 *                            |                                                                                     query (exactly 1)
	 *                            |                                                                                     default-graph-uri (0 or more)
	 *                            |                                                                                     named-graph-uri (0 or more)
	 *----------------------------------------------------------------------------------------------------------------------------------------																													
	 * query via POST directly    |   POST         default-graph-uri (0 or more)
	 *                            |                named-graph-uri (0 or more)       application/sparql-query            Unencoded SPARQL query string
	 *
	 * </pre>
	 */
	private Response post(QueryRequest req) {
		StringEntity requestEntity = null;
		HttpPost post;

		// Graphs
		String graphs = null;

		// Setting URL
		String scheme = req.getScheme();
		String host = req.getHost();
		int port = req.getPort();
		String queryPath = req.getPath();

		try {
			for (String g : req.getDefaultGraphUri()) {
				if (graphs == null)
					graphs = "default-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&default-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}
			for (String g : req.getNamedGraphUri()) {
				if (graphs == null)
					graphs = "named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}

			if (req.getHttpMethod().equals(HTTPMethod.POST)) {
				post = new HttpPost(new URI(scheme, null, host, port, queryPath, graphs, null));
				post.setHeader("Content-Type", "application/sparql-query");

				// Body
				requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
			} else {
				post = new HttpPost(new URI(scheme, null, host, port, queryPath, null, null));
				post.setHeader("Content-Type", "application/x-www-form-urlencoded");

				// Body
				if (graphs != null)
					requestEntity = new StringEntity(
							"query=" + URLEncoder.encode(req.getSPARQL(), "UTF-8") + "&" + graphs, Consts.UTF_8);
				else
					requestEntity = new StringEntity("query=" + URLEncoder.encode(req.getSPARQL(), "UTF-8"),
							Consts.UTF_8);
			}
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "URISyntaxException", e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e.getMessage());
		}

		// Set Accept header
		post.setHeader("Accept", req.getAcceptHeader());
		post.setEntity(requestEntity);

		// Set timeout
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) req.getTimeout())
				.setConnectTimeout((int) req.getTimeout()).build();
		post.setConfig(requestConfig);

		return executeRequest(post, req);
	}

	/**
	 * <pre>
	 *                               HTTP Method   Query String Parameters           Request Content Type                Request Message Body
	 *----------------------------------------------------------------------------------------------------------------------------------------
	 * query via GET              |   GET          query (exactly 1)                 None                                None
	 *                            |                default-graph-uri (0 or more)
	 *                            |                named-graph-uri (0 or more)
	 * </pre>
	 */
	private Response get(QueryRequest req) {
		String query;
		try {
			query = "query=" + URLEncoder.encode(req.getSPARQL(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.error(e1.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e1.getMessage());
		}

		String graphs = null;
		try {
			for (String g : req.getDefaultGraphUri()) {
				if (graphs == null)
					graphs = "default-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&default-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}
			for (String g : req.getNamedGraphUri()) {
				if (graphs == null)
					graphs = "named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
				else
					graphs += "&named-graph-uri=" + URLEncoder.encode(g, "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, "UnsupportedEncodingException",
					e.getMessage());
		}

		if (graphs != null)
			query += "&" + graphs;

		String url;
		// Setting URL
		String scheme = req.getScheme();
		String host = req.getHost();
		int port = req.getPort();
		String queryPath = req.getPath();

		if (port != -1)
			url = scheme + "://" + host + ":" + port + queryPath + "?" + query;
		else
			url = scheme + "://" + host + queryPath + "?" + query;

		HttpGet get;
		get = new HttpGet(url);

		// Set Accept header
		get.setHeader("Accept", req.getAcceptHeader());

		// Set timeout
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) req.getTimeout())
				.setConnectTimeout((int) req.getTimeout()).build();
		get.setConfig(requestConfig);

		return executeRequest(get, req);
	}

	@Override
	public void close() throws IOException {
		try {
			httpClient.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
}