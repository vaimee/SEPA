/* This class represents a URI RDF term
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

import it.unibo.arces.wot.sepa.commons.sparql.RDFTerm;

// TODO: Auto-generated Javadoc
/**
 * The Class RDFTermURI.
 */
public class RDFTermURI extends RDFTerm {

	/**
	 * Instantiates a new RDF term URI.
	 *
	 * @param value
	 *            the value
	 */
	public RDFTermURI(String value) {
		super(value);
		json.add("type", new JsonPrimitive("uri"));
	}
	
	/**
	 * Instantiates a new RDF term URI with null value
	 *
	 */
	public RDFTermURI() {
		this(null);
	}
}