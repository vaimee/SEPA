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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

import javax.net.ssl.SSLException;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.timing.Timings;

import org.apache.logging.log4j.Logger;

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
	protected CloseableHttpClient httpClient = HttpClients.createDefault();

	/** The security manager */
	protected SEPASecurityManager sm;

	public SPARQL11Protocol(SEPASecurityManager sm) {
		if (sm == null)
			throw new IllegalArgumentException("Security manager is null");
		this.sm = sm;
		httpClient = sm.getSSLHttpClient();
	}

	public SPARQL11Protocol() {
		httpClient = HttpClients.createDefault();
	}

	public boolean isSecure() {
		return sm != null;
	}

	private Response executeRequest(HttpUriRequest req, Request request) {
		CloseableHttpResponse httpResponse = null;
		HttpEntity responseEntity = null;
		int responseCode = 0;
		String responseBody = null;

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
				Timings.log("ENDPOINT_UPDATE_TIME", start, stop);
			else
				Timings.log("ENDPOINT_QUERY_TIME", start, stop);

			// Status code
			responseCode = httpResponse.getStatusLine().getStatusCode();

			// Body
			responseEntity = httpResponse.getEntity();
			responseBody = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
			logger.trace(String.format("Response code: %d", responseCode));
			EntityUtils.consume(responseEntity);

			// http://hc.apache.org/httpcomponents-client-4.5.x/tutorial/html/fundamentals.html#d5e279
		} catch (IOException e) {
			if (e instanceof InterruptedIOException) {
				return new ErrorResponse(HttpStatus.SC_SERVICE_UNAVAILABLE, e.getMessage());
			}
			if (e instanceof UnknownHostException) {
				return new ErrorResponse(HttpStatus.SC_NOT_FOUND, e.getMessage());
			}
			if (e instanceof ConnectTimeoutException) {
				return new ErrorResponse(HttpStatus.SC_REQUEST_TIMEOUT, e.getMessage());
			}
			if (e instanceof SSLException) {
				return new ErrorResponse(HttpStatus.SC_UNAUTHORIZED, e.getMessage());
			}
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		} finally {
			try {
				if (httpResponse != null)
					httpResponse.close();
			} catch (IOException e) {
				return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}

			responseEntity = null;
		}

		if (responseCode >= 400)
			return new ErrorResponse(responseCode, responseBody);

		if (request.getClass().equals(UpdateRequest.class))
			return new UpdateResponse(responseBody);

		try {
			return new QueryResponse(new JsonParser().parse(responseBody).getAsJsonObject());
		} catch (Exception e) {
			return new ErrorResponse(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, e.getMessage());
		}
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
			return new ErrorResponse(HttpStatus.SC_BAD_REQUEST,
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
			if (req.getHttpMethod().equals(HTTPMethod.POST)) {
				if (req.getDefaultGraphUri() != null) {
					graphs = "using-graph-uri=" + req.getDefaultGraphUri();
					if (req.getNamedGraphUri() != null) {
						graphs += "&using-named-graph-uri=" + req.getNamedGraphUri();
					}
				} else if (req.getNamedGraphUri() != null) {
					graphs = "using-named-graph-uri=" + req.getNamedGraphUri();
				}
				
				post = new HttpPost(new URI(scheme, null, host, port, updatePath, graphs, null));
				post.setHeader("Content-Type", "application/sparql-update");

				// Body
				requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
			} else {
				post = new HttpPost(new URI(scheme, null, host, port, updatePath, null, null));
				post.setHeader("Content-Type", "application/x-www-form-urlencoded");

				// Graphs 
				try {
					if (req.getDefaultGraphUri() != null) {
						graphs = "using-graph-uri=" + URLEncoder.encode(req.getDefaultGraphUri(), "UTF-8");
						if (req.getNamedGraphUri() != null) {
							graphs += "&using-named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
						}
					} else if (req.getNamedGraphUri() != null) {
						graphs = "using-named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
					}
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
				
				// Body
				if (graphs != null) requestEntity = new StringEntity("update=" + URLEncoder.encode(req.getSPARQL(), "UTF-8") + "&" + graphs,
						Consts.UTF_8);
				else requestEntity = new StringEntity("update=" + URLEncoder.encode(req.getSPARQL(), "UTF-8"),
						Consts.UTF_8);
			}
		} catch (URISyntaxException | UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		// Accept header
		post.setHeader("Accept", req.getAcceptHeader());

		// Body
		post.setEntity(requestEntity);

		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) req.getTimeout())
				.setConnectTimeout((int) req.getTimeout()).build();
		post.setConfig(requestConfig);

		return executeRequest(post, req);
	}

	// private Response patchVirtuoso(UpdateRequest req) {
	// // 1) "INSERT DATA" is not supported. Only INSERT (also if the WHERE is not
	// // present).
	// String fixedSparql = req.getSPARQL();
	// Pattern p = null;
	// try {
	// p = Pattern.compile(
	// "(?<update>.*)(delete)([^{]*)(?<udtriples>.*)(insert)([^{]*)(?<uitriples>.*)|(?<delete>.*)(delete)(?<where>[^{]*)(?<dtriples>.*)|(?<insert>.*)(insert)([^{]*)(?<itriples>.*)",
	// Pattern.CASE_INSENSITIVE);
	//
	// Matcher m = p.matcher(req.getSPARQL());
	// if (m.matches()) {
	// if (m.group("update") != null) {
	// fixedSparql = m.group("update") + " DELETE " + m.group("udtriples") + "
	// INSERT "
	// + m.group("uitriples");
	// } else if (m.group("insert") != null) {
	// fixedSparql = m.group("insert") + " INSERT " + m.group("itriples");
	// } else {
	// if (m.group("where") != null) {
	// if (m.group("where").toLowerCase().contains("where")) {
	// fixedSparql = m.group("delete") + " DELETE " + m.group("where") +
	// m.group("dtriples");
	// }
	// else
	// fixedSparql = m.group("delete") + " DELETE " + m.group("dtriples");
	// }
	// else fixedSparql = m.group("delete") + " DELETE " + m.group("dtriples");
	// }
	// }
	// } catch (Exception e) {
	// return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
	// e.getMessage());
	// }
	//
	// // 2) SPARQL 1.1 Update are issued as GET request (like for a SPARQL 1.1
	// Query)
	// String query;
	// try {
	// // custom "format" parameter
	// query = "query=" + URLEncoder.encode(fixedSparql, "UTF-8") + "&format="
	// + URLEncoder.encode(req.getAcceptHeader(), "UTF-8");
	// } catch (UnsupportedEncodingException e1) {
	// logger.error(e1.getMessage());
	// return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
	// e1.getMessage());
	// }
	//
	// // 3) Named-graphs specified like a query
	// String graphs = "";
	// try {
	// if (req.getUsingGraphUri() != null) {
	//
	// graphs += "default-graph-uri=" + URLEncoder.encode(req.getUsingGraphUri(),
	// "UTF-8");
	//
	// if (req.getUsingNamedGraphUri() != null) {
	// graphs += "&named-graph-uri=" +
	// URLEncoder.encode(req.getUsingNamedGraphUri(), "UTF-8");
	// }
	// } else if (req.getUsingNamedGraphUri() != null) {
	// graphs += "named-graph-uri=" + URLEncoder.encode(req.getUsingNamedGraphUri(),
	// "UTF-8");
	// }
	// } catch (UnsupportedEncodingException e) {
	// logger.error(e.getMessage());
	// return new ErrorResponse(req.getToken(), HttpStatus.SC_INTERNAL_SERVER_ERROR,
	// e.getMessage());
	// }
	//
	// if (!graphs.equals(""))
	// query += "&" + graphs;
	//
	// logger.debug("Query: " + query);
	//
	// // Setting URL
	// String scheme = req.getScheme();
	// String host = req.getHost();
	// int port = req.getPort();
	// String queryPath = req.getPath();
	//
	// String url;
	// if (port != -1)
	// url = scheme + "://" + host + ":" + port + queryPath + "?" + query;
	// else
	// url = scheme + "://" + host + queryPath + "?" + query;
	//
	// HttpGet get;
	// get = new HttpGet(url);
	//
	// get.setHeader("Accept", req.getAcceptHeader());
	//
	// RequestConfig requestConfig =
	// RequestConfig.custom().setSocketTimeout(req.getTimeout())
	// .setConnectTimeout(req.getTimeout()).build();
	// get.setConfig(requestConfig);
	//
	// return executeRequest(get, req);
	// }

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
			if (req.getHttpMethod().equals(HTTPMethod.POST)) {
				if (req.getDefaultGraphUri() != null) {

					graphs = "default-graph-uri=" + req.getDefaultGraphUri();

					if (req.getNamedGraphUri() != null) {
						graphs += "&named-graph-uri=" + req.getNamedGraphUri();
					}
				} else if (req.getNamedGraphUri() != null) {
					graphs = "named-graph-uri=" + req.getNamedGraphUri();
				}
				
				post = new HttpPost(new URI(scheme, null, host, port, queryPath, graphs, null));
				post.setHeader("Content-Type", "application/sparql-query");

				// Body
				requestEntity = new StringEntity(req.getSPARQL(), Consts.UTF_8);
			} else {
				post = new HttpPost(new URI(scheme, null, host, port, queryPath, null, null));
				post.setHeader("Content-Type", "application/x-www-form-urlencoded");

				try {
					if (req.getDefaultGraphUri() != null) {

						graphs = "default-graph-uri=" + URLEncoder.encode(req.getDefaultGraphUri(), "UTF-8");

						if (req.getNamedGraphUri() != null) {
							graphs += "&named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
						}
					} else if (req.getNamedGraphUri() != null) {
						graphs = "named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
					}
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage());
					return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
				
				// Body
				if (graphs != null) requestEntity = new StringEntity("query=" + URLEncoder.encode(req.getSPARQL(), "UTF-8") + "&" + graphs,
						Consts.UTF_8);
				else requestEntity = new StringEntity("query=" + URLEncoder.encode(req.getSPARQL(), "UTF-8"),
						Consts.UTF_8);
			}
		} catch (URISyntaxException | UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		post.setHeader("Accept", req.getAcceptHeader());

		post.setEntity(requestEntity);

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
			return new ErrorResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
		}

		String graphs = "";
		try {
			if (req.getDefaultGraphUri() != null) {

				graphs += "default-graph-uri=" + URLEncoder.encode(req.getDefaultGraphUri(), "UTF-8");

				if (req.getNamedGraphUri() != null) {
					graphs += "&named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
				}
			} else if (req.getNamedGraphUri() != null) {
				graphs += "named-graph-uri=" + URLEncoder.encode(req.getNamedGraphUri(), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage());
			return new ErrorResponse( HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

		if (!graphs.equals(""))
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

		get.setHeader("Accept", req.getAcceptHeader());

		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout((int) req.getTimeout())
				.setConnectTimeout((int) req.getTimeout()).build();
		get.setConfig(requestConfig);

		return executeRequest(get, req);
	}

	@Override
	public void close() {
		try {
			httpClient.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
}
