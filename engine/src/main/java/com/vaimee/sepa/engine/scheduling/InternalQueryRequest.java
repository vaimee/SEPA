/* The engine internal representation of a query request
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

package com.vaimee.sepa.engine.scheduling;

import java.util.Set;

import com.vaimee.sepa.api.commons.exceptions.SEPASparqlParsingException;
import com.vaimee.sepa.api.commons.security.ClientAuthorization;

public class InternalQueryRequest extends InternalUQRequest {
	private String internetMediaType = "application/sparql-results+json";

	public InternalQueryRequest(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth) throws SEPASparqlParsingException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
	}

	public InternalQueryRequest(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth, String mediaType) throws SEPASparqlParsingException {
		this(sparql, defaultGraphUri, namedGraphUri, auth);

		internetMediaType = mediaType;
	}

	public String getInternetMediaType() {
		return internetMediaType;
	}

	@Override
	public String toString() {
		return "*QUERY* RDF DATA SET: {" + rdfDataSet + " USING GRAPHS: " + defaultGraphUri + " NAMED GRAPHS: "
				+ namedGraphUri + "} SPARQL: " + sparql;
	}

	@Override
	protected Set<String> getGraphURIs(String sparql) throws SEPASparqlParsingException {
		return new JenaSparqlParsing().getQueryGraphURIs(sparql);
	}
}
