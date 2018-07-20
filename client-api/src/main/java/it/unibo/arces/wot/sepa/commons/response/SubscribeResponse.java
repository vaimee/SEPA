/* This class represents the response to a subscribe request
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
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * This class represents the response to a SPARQL 1.1 Subscribe (see SPARQL 1.1
 * Subscription Language)
 *
 * The JSON serialization is the following:
 *
 * {"notification" : {"spuid":"SPUID","alias":"ALIAS"(optional),"addedResults":<BindingsResults>,"removedResults:{},"sequence":0}}
 */

public class SubscribeResponse extends Response {

	public BindingsResults getBindingsResults() {
		return new BindingsResults(json.get("notification").getAsJsonObject().get("addedResults").getAsJsonObject());
	}

	/**
	 * Instantiates a new subscribe response.
	 *
	 * @param token
	 *            the token
	 * @param spuid
	 *            the spuid
	 * @param alias
	 *            the alias
	 */
	public SubscribeResponse(String spuid, String alias,BindingsResults firstResults) {
		super();

		JsonObject response = new JsonObject();
		
		if (spuid == null) throw new IllegalArgumentException("SPUID is null");
		response.add("spuid", new JsonPrimitive(spuid));
		
		if (alias != null)
			response.add("alias", new JsonPrimitive(alias));
		
		response.add("sequence", new JsonPrimitive(0));
		
		response.add("addedResults", new BindingsResults(firstResults).toJson());
		if (firstResults != null) response.add("removedResults", new BindingsResults(firstResults.getVariables(),null).toJson());
		else response.add("removedResults", new JsonObject());
				
		json.add("notification", response);
	}

	public SubscribeResponse(JsonObject jsonMessage) {
		json = jsonMessage;
	}

	/**
	 * Gets the spuid.
	 *
	 * @return the spuid
	 */
	public String getSpuid() {
		try {
			return json.get("notification").getAsJsonObject().get("spuid").getAsString();
		}
		catch(Exception e) {
			return "";
		}
	}

	/**
	 * Gets the alias.
	 *
	 * @return the alias
	 */
	public String getAlias() {
		try {
			return json.get("notification").getAsJsonObject().get("alias").getAsString();
		}
		catch(Exception e) {
			return "";
		}
	}
}
