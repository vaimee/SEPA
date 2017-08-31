/* This class represents the response to a unsubscribe request
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

// TODO: Auto-generated Javadoc
/**
 * This class represents the response to a SPARQL 1.1 Unsubscribe (see SPARQL
 * 1.1 Subscription Language)
 *
 * The JSON serialization is the following:
 *
 * {"unsubscribed" : "SPUID"}
 *
 */

public class UnsubscribeResponse extends Response {

	/**
	 * Instantiates a new unsubscribe response.
	 *
	 * @param token
	 *            the token
	 * @param spuid
	 *            the spuid
	 */
	public UnsubscribeResponse(Integer token, String spuid) {
		super(token);

		if (spuid != null)
			json.add("unsubscribed", new JsonPrimitive(spuid));
	}

	/**
	 * Instantiates a new unsubscribe response.
	 *
	 * @param spuid
	 *            the spuid
	 */
	public UnsubscribeResponse(String spuid) {
		super();

		if (spuid != null)
			json.add("unsubscribed", new JsonPrimitive(spuid));
	}

	/**
	 * Instantiates a new unsubscribe response.
	 */
	public UnsubscribeResponse() {
		super();
	}

	public UnsubscribeResponse(JsonObject notify) {
		json = notify;
	}

	/**
	 * Gets the spuid.
	 *
	 * @return the spuid
	 */
	public String getSpuid() {
		if (json.get("unsubscribed") == null)
			return "";
		return json.get("unsubscribed").getAsString();
	}
}
