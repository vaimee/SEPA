/* This class represents the results of a SPARQL 1.1 Query
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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.logging.Logging;

// TODO: Auto-generated Javadoc
/**
 * This class represents the results of a SPARQL 1.1 Query
 * <p>
 * This conforms with the following: - SPARQL 1.1 Query Results JSON Format
 * https://www.w3.org/TR/2013/REC-sparql11-results-json-20130321/
 * <p>
 * It uses https://github.com/google/gso as internal representation of the
 * results in JSON format
 * 
 */

public class BindingsResults {

	/** The results. */
	private JsonObject results;

	/**
	 * Instantiates a new bindings results.
	 *
	 * @param results the results
	 */
	public BindingsResults(JsonObject results) {
		this.results = results;
	}

	/**
	 * Instantiates a new bindings results.
	 *
	 * @param varSet    the var set
	 * @param solutions the solutions
	 */
	public BindingsResults(ArrayList<String> varSet, List<Bindings> solutions) {
		results = new JsonObject();

		JsonObject vars = new JsonObject();
		JsonArray varArray = new JsonArray();
		if (varSet != null)
			for (String var : varSet) {
				varArray.add(var);
			}
		vars.add("vars", varArray);
		results.add("head", vars);

		JsonArray bindingsArray = new JsonArray();
		if (solutions != null)
			for (Bindings solution : solutions)
				bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}

	/**
	 * Instantiates a new bindings results.
	 *
	 * @param newBindings the new bindings
	 */
	public BindingsResults(BindingsResults newBindings) {
		results = new JsonObject();

		JsonObject vars = new JsonObject();
		JsonArray varArray = new JsonArray();
		if (newBindings != null)
			for (String var : newBindings.getVariables()) {
				varArray.add(var);
			}
		vars.add("vars", varArray);
		results.add("head", vars);

		JsonArray bindingsArray = new JsonArray();
		if (newBindings != null)
			for (Bindings solution : newBindings.getBindings())
				bindingsArray.add(solution.toJson());
		JsonObject bindings = new JsonObject();
		bindings.add("bindings", bindingsArray);
		results.add("results", bindings);
	}

	public boolean isAskResult() {
		return (results.has("boolean") && results.has("head"));
	}

	public boolean getAskBoolean() {
		return results.get("boolean").getAsBoolean();
	}

	/**
	 * Gets the variables.
	 *
	 * @return the variables
	 */
	public ArrayList<String> getVariables() {
		ArrayList<String> vars = new ArrayList<String>();
		JsonArray variables = getVariablesArray();
		if (variables == null)
			return vars;

		for (JsonElement var : variables)
			vars.add(var.getAsString());
		return vars;
	}

	/**
	 * Gets the bindings.
	 *
	 * @return the bindings
	 */
	public List<Bindings> getBindings() {
		List<Bindings> list = new ArrayList<Bindings>();
		JsonArray bindings = getBindingsArray();
		if (bindings == null)
			return list;

		for (JsonElement solution : bindings) {
			list.add(new Bindings(solution.getAsJsonObject()));
		}
		return list;
	}

	/**
	 * To json.
	 *
	 * @return the json object
	 */
	public JsonObject toJson() {
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return results.toString();
	}

	/**
	 * Checks if is empty.
	 *
	 * @return true, if is empty
	 */
	public boolean isEmpty() {
		JsonArray bindings = getBindingsArray();
		if (bindings == null)
			return true;
		if (bindings.size() == 1) {
			if (bindings.get(0).getAsJsonObject().entrySet().isEmpty())
				return true;
		}
		return (bindings.size() == 0);
	}

	/**
	 * Adds the.
	 *
	 * @param binding the binding
	 */
	public void add(Bindings binding) {
		if (binding == null)
			return;
		JsonArray bindings = getBindingsArray();
		if (bindings == null)
			return;

		bindings.add(binding.toJson());
	}

	/**
	 * Contains.
	 *
	 * @param solution the solution
	 * @return true, if successful
	 */
	public boolean contains(Bindings solution) {
//		JsonArray solutionSet = getBindingsArray();
//		JsonObject bindings = solution.toJson();
//
//		for (int i = 0; i < solutionSet.size(); i++) {
//			JsonObject solutionX = solutionSet.get(i).getAsJsonObject();
//			boolean match = true;
//
//			for (String key : bindings.keySet()) {
//				if (!solutionX.has(key) || !solutionX.get(key).getAsString().equals(bindings.get(key).getAsString())) {
//					match = false;
//					break;
//				}
//			}
//
//			if (match) {
//				return true;
//			}
//		}
//		return false;

		if (solution == null) return false;
		
		JsonArray bindings = getBindingsArray();
		if (bindings == null) return false;		
		if (bindings.size() == 0) return false;
		
		if(getVariables().size() != solution.getVariables().size()) return false;
		if(!getVariables().containsAll(solution.getVariables())) return false;
		
				
		for (JsonElement b : bindings) {
			Bindings temp = new Bindings(b.getAsJsonObject());
			boolean found = true;
			for(String var : solution.getVariables()) {
				try {
					// Comparing BNODE between two different queries is not possible
					if (solution.isBNode(var) && temp.isBNode(var)) continue;
					if(!solution.getRDFTerm(var).equals(temp.getRDFTerm(var))) {
						found = false;
						break;
					}
				} catch (SEPABindingsException e) {
					Logging.error(e.getMessage());
					return false;
				}						
			}
			if (found) return true;
		}
		
		return false;
	}

	/**
	 * Removes the.
	 *
	 * @param solution the solution
	 */
	public void remove(Bindings solution) {
		if (solution == null)
			return;
		JsonArray bindings = getBindingsArray();
		if (bindings == null)
			return;

		bindings.remove(solution.toJson());
	}

	/**
	 * Size.
	 *
	 * @return the int
	 */
	public int size() {
		JsonArray bindings = getBindingsArray();
		if (bindings == null)
			return 0;
		return bindings.size();
	}

	/**
	 * Gets the bindings array.
	 *
	 * @return the bindings array
	 */
	private JsonArray getBindingsArray() {
		JsonElement varArray;
		if (results == null)
			return null;
		if ((varArray = results.get("results")) == null)
			return null;
		if ((varArray = varArray.getAsJsonObject().get("bindings")) == null)
			return null;

		return varArray.getAsJsonArray();
	}

	/**
	 * Gets the variables array.
	 *
	 * @return the variables array
	 */
	private JsonArray getVariablesArray() {
		JsonElement varArray;
		if (results == null)
			return null;
		if ((varArray = results.get("head")) == null)
			return null;
		if ((varArray = varArray.getAsJsonObject().get("vars")) == null)
			return null;

		return varArray.getAsJsonArray();
	}
}