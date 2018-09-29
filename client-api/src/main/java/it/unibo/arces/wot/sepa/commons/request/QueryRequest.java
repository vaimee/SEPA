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

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;

/**
 * This class represents a SPARQL 1.1 Query request
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query</a>
* */

public class QueryRequest extends SPARQL11Request {
	/**
	 * Instantiates a new query request.
	 *
	 * @param sparql the <a href="https://www.w3.org/TR/sparql11-query/">SPARQL 1.1 Query</a>
	 */
	
	public QueryRequest(HTTPMethod method,String scheme,String host, int port, String path,String sparql,String default_graph_uri,String named_graph_uri,String authorization,long timeout) {
		super(sparql,authorization,default_graph_uri,named_graph_uri,timeout);
		
		this.method = method;
		this.host = host;
		this.port = port;
		this.path = path;
		this.timeout = timeout;
		this.scheme = scheme;
	}

	public String getAcceptHeader() {
		return "application/sparql-results+json";
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
}
