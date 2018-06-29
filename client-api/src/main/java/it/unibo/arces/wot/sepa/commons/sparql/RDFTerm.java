/* This class represents a RDF term (URI, literal or blank node)
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

import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;

// TODO: Auto-generated Javadoc
/**
 * The Class RDFTerm.
 */
public abstract class RDFTerm {

	/** The json. */
	protected JsonObject json = null;

	public boolean isURI() {
		return this.getClass().equals(RDFTermURI.class);
	}
	
	public boolean isLiteral() {
		return this.getClass().equals(RDFTermLiteral.class);
	}
	
	public boolean isBNode() {
		return this.getClass().equals(RDFTermBNode.class);
	}
	
	/**
	 * Gets the value.
	 *
	 * @return the value or null if tha value has not been assigned
	 */
	public String getValue() {
		if (!json.has("value")) return null;
		return json.get("value").getAsString();
	}

	/**
	 * Equals.
	 *
	 * @param t
	 *            the t
	 * @return true, if successful
	 */
	public boolean equals(RDFTerm t) {
		return this.json.equals(t.toJson());
	}

	/**
	 * To json.
	 *
	 * @return the json object
	 */
	public JsonObject toJson() {
		return json;
	}

	/**
	 * Instantiates a new RDF term.
	 *
	 * @param value
	 *            the value
	 */
	public RDFTerm(String value) throws IllegalArgumentException {
		json = new JsonObject();
		
		if (value != null) json.add("value", new JsonPrimitive(value));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return json.toString();
	}
}
