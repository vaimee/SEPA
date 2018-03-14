/* This class represents a SEPA notification
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

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;

// TODO: Auto-generated Javadoc
/**
 * This class represents a SPARQL Notification (see SPARQL 1.1 Subscription
 * Language)
 *
 * The JSON serialization looks like: {@code
 * { "notification":{"spuid" : "SPUID" , "sequence" : "SEQUENTIAL NUMBER", "addedResults" : <JSON
 * Notification Results>, "removedResults" : <JSON Notification Results> }} }
 * 
 */

public class Notification extends Response {
	private ARBindingsResults results;
	/**
	 * Instantiates a new notification.
	 *
	 * @param spuid
	 *            the spuid
	 * @param results
	 *            the results
	 * @param sequence
	 *            the sequence
	 */
	public Notification(String spuid, ARBindingsResults results, Integer sequence) {
		super();

		this.results = results;
		
		JsonObject response = new JsonObject();
		
		if (results != null) {
			response.add("addedResults", results.getAddedBindings().toJson());
			response.add("removedResults", results.getRemovedBindings().toJson());
		}
		if (spuid != null)
			response.add("spuid", new JsonPrimitive(spuid));
		response.add("sequence", new JsonPrimitive(sequence));
		
		json.add("notification", response);
	}

	/**
	 * Instantiates a new notification.
	 *
	 * @param notify
	 *            the notify
	 */
	public Notification(JsonObject notify) {
		super();

		json = notify;
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
	 * Gets the AR bindings results.
	 *
	 * @return the AR bindings results
	 */
	public ARBindingsResults getARBindingsResults() {
		return results; 
//		if (json.getAsJsonObject("results") != null)
//			return new ARBindingsResults(json.getAsJsonObject("results"));
//		return null;
	}

	/**
	 * Gets the sequence.
	 *
	 * @return the sequence
	 */
	public Integer getSequence() {
		
		return json.get("sequence").getAsInt();
	}

	// /**
	// * To be notified.
	// *
	// * @return true, if successful
	// */
	// public boolean toBeNotified() {
	// return json.get("results") != null;
	// }
}
