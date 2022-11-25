/* This class represents the response to a SPARQL 1.1 query
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
package it.unibo.arces.wot.sepa.commons.response;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * This class represents the results of a SPARQL 1.1 Query
 */

public class QueryResponse extends Response {
	/**
	 * Instantiates a new query response.
	 *
	 * @param body the body
	 */

	private String responseBody;

	public QueryResponse(String responseBody) {
		super();

		try {
			json = JsonParser.parseString(responseBody).getAsJsonObject();
		} catch (JsonParseException e) {
			json = null;
		}

		this.responseBody = responseBody;
	}

	/**
	 * Gets the bindings results.
	 *
	 * @return the bindings results
	 */
	public BindingsResults getBindingsResults() {
		if (json == null)
			return null;

		return new BindingsResults(json.getAsJsonObject());
	}

	@Override
	public String toString() {
		return responseBody;
	}
}
