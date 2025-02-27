/* This class represents a blank node RDF term
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

package com.vaimee.sepa.commons.sparql;

import com.google.gson.JsonPrimitive;

/**
 * The Class RDFTermBNode.
 */
public class RDFTermBNode extends RDFTerm {

	/**
	 * Instantiates a new RDF term B node.
	 *
	 * @param value
	 *            the value
	 */
	public RDFTermBNode(String value) {
		super(value);
		
		json.add("type", new JsonPrimitive("bnode"));
	}
	
	/**
	 * Equals.
	 *
	 * @param t
	 *            the t
	 * @return true, if successful
	 * 
	 * https://www.w3.org/TR/rdf-concepts/#section-blank-nodes
	 * 
	 * 6.6 Blank Nodes

The blank nodes in an RDF graph are drawn from an infinite set. 

This set of blank nodes, the set of all RDF URI references and the set of all literals are pairwise disjoint.

Otherwise, this set of blank nodes is arbitrary.

RDF makes no reference to any internal structure of blank nodes. 

**** Given two blank nodes, it is possible to determine whether or not they are the same. ****

References:
- https://www.w3.org/DesignIssues/Diff (Tim Berners Lee 2001)


	 */
	@Override
	public boolean equals(Object t) {
		if (t == this)
			return true;
		if (!t.getClass().equals(RDFTermBNode.class))
			return false;

		return this.getValue().equals(((RDFTermBNode) t).getValue());
	}
}
