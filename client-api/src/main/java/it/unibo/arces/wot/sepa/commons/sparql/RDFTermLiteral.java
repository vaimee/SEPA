/* This class represents a literal RDF term
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

/**
 * The Class RDFTermLiteral.
 */
public class RDFTermLiteral extends RDFTerm {

	/**
	 * Instantiates a new RDF term literal with a null value
	 *
	 */
	public RDFTermLiteral() {
		this(null,null,null);
	}
	
	/**
	 * Instantiates a new RDF term literal.
	 *
	 * @param value
	 *            the value
	 */
	public RDFTermLiteral(String value) {
		this(value,null,null);
	}

	/**
	 * Instantiates a new RDF term literal.
	 * 
	 * @param value the value
	 * 
	 * @param datatype a XSD datatype (https://www.w3.org/TR/xmlschema-2/) 
	 * 
	 * @param language if not null, it is a language identifier (RFC3066 (http://www.ietf.org/rfc/rfc3066.txt)) and the datatype is set to "xsd:string" by default
	 */
	public RDFTermLiteral(String value, String datatype, String language) {
		super(value);

		json.add("type", new JsonPrimitive("literal"));
		
		if (language != null) {
			json.add("xml:lang", new JsonPrimitive(language));
			json.add("datatype", new JsonPrimitive("xsd:string"));
		}
		else if (datatype != null) json.add("datatype", new JsonPrimitive(datatype));
		else json.add("datatype", new JsonPrimitive("xsd:string"));
	}

	/**
	 * Instantiates a new RDF term literal.
	 * 
	 * @param value the value
	 * 
	 * @param datatype an XSD datatype (https://www.w3.org/TR/xmlschema-2/)
	 */
	public RDFTermLiteral(String value, String datatype) {
		this(value,datatype,null);
	}
	
	/**
	 * Gets the language tag.
	 *
	 * @return the language tag or null if it is not defined
	 */
	public String getLanguageTag() {
		if (!json.has("xml:lang")) return null;
		return json.get("xml:lang").getAsString();
	}

	/**
	 * Gets the datatype.
	 *
	 * @return the datatype or null if it is not defined
	 */
	public String getDatatype() {
		if (!json.has("datatype")) return null;
		return json.get("datatype").getAsString();
	}
}
