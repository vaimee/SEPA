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

package com.vaimee.sepa.api.commons.sparql;

import com.google.gson.JsonObject;

/**
 * This class represents the content of a SEPA notification
 * <p>
 * It includes the added and removed bindings since the previous notification.
 */

public class ARBindingsResults {

	private BindingsResults added;
	private BindingsResults removed;
	
	/**
	 * Instantiates a new AR bindings results.
	 *
	 * @param added
	 *            the added
	 * @param removed
	 *            the removed
	 */
	public ARBindingsResults(BindingsResults added, BindingsResults removed) {
		this.added = added;
		this.removed = removed;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	// Serialized according to the SPARQL 1.1 SE Notification JSON format
	public String toString() {
		return toJson().toString();
	}

	/**
	 * To json.
	 *
	 * @return the json object
	 */
	public JsonObject toJson() {
		JsonObject ar = new JsonObject();
		ar.add("addedResults", added.toJson());
		ar.add("removedResults", removed.toJson());
		return ar;
	}

	/**
	 * Gets the added bindings.
	 *
	 * @return the added bindings
	 */
	public BindingsResults getAddedBindings() {
		return added;
	}

	/**
	 * Gets the removed bindings.
	 *
	 * @return the removed bindings
	 */
	public BindingsResults getRemovedBindings() {
		return removed;
	}
}
