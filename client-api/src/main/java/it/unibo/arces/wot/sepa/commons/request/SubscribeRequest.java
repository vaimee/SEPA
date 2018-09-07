/* This class represents a subscribe request
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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * This class represents the request of performing a SPARQL 1.1 Subscribe
 */

public class SubscribeRequest extends Request {

	/** The alias. */
	private String alias = null;

	protected String default_graph_uri = null;
	protected String named_graph_uri = null;
	
	public SubscribeRequest(String sparql, String alias, String defaultGraphURI, String namedGraphURI,
			String authorization,long timeout) {
		super(sparql, authorization,timeout);
		
		this.alias = alias;	
		this.default_graph_uri = defaultGraphURI;
		this.named_graph_uri = namedGraphURI;
	}

	@Override
	public String toString() {
		// Create SPARQL 1.1 Subscribe JSON request
		JsonObject body = new JsonObject();
		JsonObject request = new JsonObject();
		body.add("sparql", new JsonPrimitive(getSPARQL()));
		if (getAuthorizationHeader() != null)
			body.add("authorization", new JsonPrimitive(getAuthorizationHeader()));
		if (getAlias() != null)
			body.add("alias", new JsonPrimitive(getAlias()));
		if (default_graph_uri != null) {
			body.add("default-graph-uri", new JsonPrimitive(default_graph_uri));
		}
		if (named_graph_uri != null) {
			body.add("named-graph-uri", new JsonPrimitive(named_graph_uri));
		}
		body.add("timeout", new JsonPrimitive(getTimeout()));
		
		request.add("subscribe", body);

		return request.toString();
	}

	/**
	 * This method returns the alias of the subscription.
	 * 
	 * @return The subscription alias or <i>null</i> is not present
	 */
	public String getAlias() {
		return alias;
	}
	
	/**
	 * Default implementation. Two requests are equal if they belong to the same class and their SPARQL strings are equals. SPARQL matching should be based on SPARQL algebra
	 * and SPARQL semantics. The default implementation provides a syntax based matching. 
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SubscribeRequest)) return false;
		return sparql.equals(((SubscribeRequest)obj).sparql);
	}
}
