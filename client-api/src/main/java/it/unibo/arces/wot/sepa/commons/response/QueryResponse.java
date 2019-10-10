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

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * This class represents the results of a SPARQL 1.1 Query
 */

public class QueryResponse extends Response {
	/**
	 * Instantiates a new query response.
	 *
	 * @param body
	 *            the body
	 */
	public QueryResponse(JsonObject bindingResultsJSON) {
		super();
		json = bindingResultsJSON;
	}

	/**
	 * Gets the bindings results.
	 *
	 * @return the bindings results
	 */
	public BindingsResults getBindingsResults() {	
		if (json == null) return null;
		
		return new BindingsResults(json.getAsJsonObject());
	}
	
	public boolean getAskResult() throws SEPABindingsException {
		if (json == null) throw new SEPABindingsException("Response is null");
		if (!json.has("boolean")) throw new SEPABindingsException("Response format do not conform with ASK");
		
		return json.get("boolean").getAsBoolean();
	}
}
