/* This class represents a query request
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

package it.unibo.arces.wot.sepa.commons.request;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;

/**
 * This class represents a SPARQL 1.1 Query request
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query</a>
* */

public class QueryRequest extends Request {
	protected String default_graph_uri = null;
	protected String named_graph_uri = null;
	
	/**
	 * Instantiates a new query request.
	 *
	 * @param token the token of the request
	 * @param sparql the <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query</a>
	 */
	public QueryRequest(Integer token, String sparql) {
		super(token, sparql);
	}
	
	/**
	 * Instantiates a new query request.
	 *
	 * @param sparql the <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query</a>
	 */
	public QueryRequest(String sparql) {
		super(sparql);
	}

	public QueryRequest(String sparql,String default_graph_uri,String named_graph_uri) {
		super(sparql);
		
		this.default_graph_uri = default_graph_uri;
		this.named_graph_uri = named_graph_uri;
	}
	
	public QueryRequest(Integer token,HTTPMethod method,String scheme,String host, int port, String path,String sparql,int timeout,String default_graph_uri,String named_graph_uri,String authorization) {
		super(token,sparql);
		
		super.method = method;
		super.host = host;
		super.port = port;
		super.path = path;
		super.timeout = timeout;
		super.scheme = scheme;
		
		super.authorizationHeader = authorization;
		
		this.default_graph_uri = default_graph_uri;
		this.named_graph_uri = named_graph_uri;
	}
	
	public QueryRequest(HTTPMethod queryMethod, String queryProtocolScheme, String queryHost, int queryPort,
			String queryPath, String string, int timeout, String defaultGraphURI, String namedGraphURI,String authorization) {
		this(-1,queryMethod,  queryProtocolScheme,  queryHost,  queryPort,queryPath,  string,  timeout,  defaultGraphURI,  namedGraphURI,authorization);
	}
	
	@Override
	public String toString() {
		if (token != -1) return "QUERY #"+token+" "+sparql;
		return "QUERY "+sparql;
		
	}

	public String getAcceptHeader() {
		return "application/sparql-results+json";
	}

	public String getDefaultGraphUri() {
		if (default_graph_uri == null) return null;
		try {
			return URLDecoder.decode(default_graph_uri,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public String getNamedGraphUri() {
		if (named_graph_uri == null) return null;
		try {
			return URLDecoder.decode(named_graph_uri,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * Default implementation. Two requests are equal if they belong to the same class and their SPARQL strings are equals. SPARQL matching should be based on SPARQL algebra
	 * and SPARQL semantics. The default implementation provides a syntax based matching. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof QueryRequest)) return false;
		return sparql.equals(((QueryRequest)obj).sparql);
	}
	
	@Override
	public int hashCode() {
		return sparql.hashCode();
	}
}
