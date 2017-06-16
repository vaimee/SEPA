/* This class represents the content of a notification
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

package it.unibo.arces.wot.sepa.commons.sparql;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

// TODO: Auto-generated Javadoc
/**
 * This class represents the content of a SEPA notification
 * 
 * It includes the added and removed bindings since the previous notification.
 */

public class ARBindingsResults {

	/** The results. */
	JsonObject results = new JsonObject();

	/**
	 * Instantiates a new AR bindings results.
	 *
	 * @param results
	 *            the results
	 */
	public ARBindingsResults(JsonObject results) {
		this.results = results;
	}

	/**
	 * Instantiates a new AR bindings results.
	 *
	 * @param added
	 *            the added
	 * @param removed
	 *            the removed
	 */
	public ARBindingsResults(BindingsResults added, BindingsResults removed) {
		JsonObject nullResults = new JsonObject();
		JsonArray arr = new JsonArray();
		nullResults.add("bindings", arr);

		JsonObject nullHead = new JsonObject();
		nullHead.add("vars", new JsonArray());

		JsonObject addedResults = nullResults;
		JsonObject removedResults = nullResults;
		JsonObject head = nullHead;

		if (added != null) {
			head = added.toJson().get("head").getAsJsonObject();
			addedResults = added.toJson().get("results").getAsJsonObject();
		}

		if (removed != null) {
			head = removed.toJson().get("head").getAsJsonObject();
			removedResults = removed.toJson().get("results").getAsJsonObject();
		}

		results.add("addedresults", addedResults);
		results.add("removedresults", removedResults);
		results.add("head", head);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	// Serialized according to the SPARQL 1.1 SE Notification JSON format
	public String toString() {
		return results.toString();
	}

	/**
	 * To json.
	 *
	 * @return the json object
	 */
	public JsonObject toJson() {
		return results;
	}

	/**
	 * Gets the added bindings.
	 *
	 * @return the added bindings
	 */
	public BindingsResults getAddedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("addedresults"));
		ret.add("head", results.get("head"));
		return new BindingsResults(ret);
	}

	/**
	 * Gets the removed bindings.
	 *
	 * @return the removed bindings
	 */
	public BindingsResults getRemovedBindings() {
		JsonObject ret = new JsonObject();
		ret.add("results", results.get("removedresults"));
		ret.add("head", results.get("head"));
		return new BindingsResults(ret);
	}
}
