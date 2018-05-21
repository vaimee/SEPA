/* This class represents a query solution of a SPARQL 1.1 Query
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

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// TODO: Auto-generated Javadoc
/**
 * This class represents a query solution of a SPARQL 1.1 Query
 * 
 * An example of the internal representation as JSON object follows:
 * {@code
 * { "x" : { "type": "bnode", "value": "r2" }, "hpage" : { "type": "uri",
 * "value": "http://work.example.org/alice/" }, "blurb" : { "datatype":
 * "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral", "type": "literal",
 * "value": "<p xmlns=\"http://www.w3.org/1999/xhtml\">My name is <b>alice</b>
 * </p>
 * " }, "name" : { "type": "literal", "value": "Bob", "xml:lang": "en" } }}
 * 
 */

public class Bindings {

	/** The solution. */
	private JsonObject solution;

	/**
	 * Instantiates a new bindings.
	 *
	 * @param solution
	 *            the solution
	 */
	public Bindings(JsonObject solution) {
		this.solution = solution;
	}

	/**
	 * Instantiates a new bindings.
	 */
	public Bindings() {
		solution = new JsonObject();
	}

	/**
	 * Gets the variables.
	 *
	 * @return the variables
	 */
	public Set<String> getVariables() {
		Set<String> ret = new HashSet<String>();
		for (Entry<String, JsonElement> entry : solution.entrySet()) {
			ret.add(entry.getKey());
		}
		return ret;
	}

	/**
	 * Gets the binding value.
	 *
	 * @param variable
	 *            the variable
	 * @return the binding value
	 */
	public String getBindingValue(String variable) {
		if (solution.get(variable) == null)
			return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("value").getAsString();
	}

	/**
	 * Gets the datatype.
	 *
	 * @param variable
	 *            the variable
	 * @return the datatype
	 */
	public String getDatatype(String variable) {
		if (solution.get(variable) == null)
			return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		if (json.get("datatype") == null)
			return null;
		return json.get("datatype").getAsString();
	}

	/**
	 * Gets the language.
	 *
	 * @param variable
	 *            the variable
	 * @return the language
	 */
	public String getLanguage(String variable) {
		if (solution.get(variable) == null)
			return null;
		JsonObject json = solution.get(variable).getAsJsonObject();
		if (json.get("xml:lang") == null)
			return null;
		return json.get("xml:lang").getAsString();
	}

	/**
	 * Checks if is literal.
	 *
	 * @param variable
	 *            the variable
	 * @return true, if is literal
	 */
	public boolean isLiteral(String variable) {
		if (solution.get(variable) == null)
			return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("literal");
	}

	/**
	 * Checks if is uri.
	 *
	 * @param variable
	 *            the variable
	 * @return true, if is uri
	 */
	public boolean isURI(String variable) {
		if (solution.get(variable) == null)
			return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("uri");
	}

	/**
	 * Checks if is b node.
	 *
	 * @param variable
	 *            the variable
	 * @return true, if is b node
	 */
	public boolean isBNode(String variable) {
		if (solution.get(variable) == null)
			return false;
		JsonObject json = solution.get(variable).getAsJsonObject();
		return json.get("type").getAsString().equals("bnode");
	}

	/**
	 * Adds the binding.
	 *
	 * @param variable
	 *            the variable
	 * @param value
	 *            the value
	 */
	public void addBinding(String variable, RDFTerm value) {
		solution.add(variable, value.toJson());
	}

	/**
	 * Equals.
	 *
	 * @param qs
	 *            the qs
	 * @return true, if successful
	 */
	public boolean equals(Bindings qs) {
		return this.solution.equals(qs.solution);
	}

	/**
	 * To json.
	 *
	 * @return the json object
	 */
	public JsonObject toJson() {
		return solution;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return solution.toString();
	}

	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		return solution.isJsonNull();
	}
}
