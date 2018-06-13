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

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;

// TODO: Auto-generated Javadoc
/**
 * This class represents the request of performing a SPARQL 1.1 Subscribe
 */

public class SubscribeRequest extends QueryRequest {

	/** The alias. */
	private String alias = null;

	public SubscribeRequest(String sparql, String alias, String defaultGraphURI, String namedGraphURI,
			String authorization) {
		this(-1, sparql, alias, defaultGraphURI, namedGraphURI, authorization);
	}

	public SubscribeRequest(Integer token, String sparql, String alias, String defaultGraphURI, String namedGraphURI,
			String authorization) {
		super(token, sparql);
		
		this.alias = alias;
		
		default_graph_uri = defaultGraphURI;
		named_graph_uri = namedGraphURI;
		authorizationHeader = authorization;
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
		if (getDefaultGraphUri() != null) {
			body.add("default-graph-uri", new JsonPrimitive(getDefaultGraphUri()));
		}
		if (getNamedGraphUri() != null) {
			body.add("named-graph-uri", new JsonPrimitive(getNamedGraphUri()));
		}
		
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
}
