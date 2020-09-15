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

package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;

import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;

public class InternalQueryRequest extends InternalUQRequest {
	private String internetMediaType = "application/sparql-results+json";

	public InternalQueryRequest(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth) throws QueryException {
		super(sparql, defaultGraphUri, namedGraphUri, auth);
	}

	public InternalQueryRequest(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,
			ClientAuthorization auth, String mediaType) throws QueryException {
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

	protected Set<String> getGraphURIs(String sparql) throws QueryException {
		Set<String> ret = new HashSet<>();

		if (sparql == null)
			return ret;

		Query q = null;
		logger.trace("Parsing query: " + sparql);
		q = QueryFactory.create(sparql);

		logger.trace("Get dataset descriptiors");
		if (q.hasDatasetDescription()) {
			logger.trace("Get default graph URIs");
			for (String gr : q.getDatasetDescription().getDefaultGraphURIs()) {
				ret.add(gr);
			}
			logger.trace("Get named graph URIs");
			for (String gr : q.getDatasetDescription().getNamedGraphURIs()) {
				ret.add(gr);
			}
		}

		logger.trace("Get graph URIs");
		List<String> graphs = q.getGraphURIs();
		logger.trace("Get named graph URIs");
		List<String> namedGraphs = q.getNamedGraphURIs();

		ret.addAll(extractGraphs(q.getQueryPattern()));
		ret.addAll(graphs);
		ret.addAll(namedGraphs);

		return ret;
	}

	private Set<String> extractGraphs(Element e) {
		Set<String> ret = new HashSet<String>();

		if (e == null)
			return ret;

		logger.trace("Extract graphs " + e);
		if (e.getClass().equals(ElementGroup.class)) {
			ElementGroup group = (ElementGroup) e;
			for (Element element : group.getElements()) {
				ret.addAll(extractGraphs(element));
			}
		} else if (e.getClass().equals(ElementNamedGraph.class)) {
			ElementNamedGraph namedGraph = (ElementNamedGraph) e;
			if (namedGraph.getGraphNameNode().isURI())
				ret.add(namedGraph.getGraphNameNode().getURI());
			// TODO: comment if variables can be only NAMED graphs
			else
				ret.add("*");
		}

		return ret;
	}
}
