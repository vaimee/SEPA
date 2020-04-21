/* The engine internal representation of an update request
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

import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.ParserSPARQL11Update;
import org.apache.jena.sparql.modify.UpdateRequestSink;
import org.apache.jena.sparql.modify.request.UpdateBinaryOp;
import org.apache.jena.sparql.modify.request.UpdateCreate;
import org.apache.jena.sparql.modify.request.UpdateData;
import org.apache.jena.sparql.modify.request.UpdateDeleteWhere;
import org.apache.jena.sparql.modify.request.UpdateDropClear;
import org.apache.jena.sparql.modify.request.UpdateLoad;
import org.apache.jena.sparql.modify.request.UpdateModify;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;

import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;

public class InternalUpdateRequest extends InternalUQRequest {
	public InternalUpdateRequest(String sparql, Set<String> defaultGraphUri, Set<String> namedGraphUri,ClientAuthorization auth) throws SPARQL11ProtocolException {
		super(sparql, defaultGraphUri, namedGraphUri,auth);
	}

	@Override
	public String toString() {
		return "*UPDATE* {RDF DATA SET: "+rdfDataSet +" USING GRAPHS: "+ defaultGraphUri + " NAMED GRAPHS: " + namedGraphUri+"} SPARQL: " +sparql ;
	}
	
	/*
	 * 2.2.3 Specifying an RDF Dataset
	 * 
	 * SPARQL Update requests are executed against a Graph Store, a mutable container of RDF graphs managed by a SPARQL service. 
	 * 
	 * The WHERE clause of a SPARQL update DELETE/INSERT operation [UPDATE] matches against data in an RDF Dataset, which is a subset of the Graph Store. 
	 * 
	 * The RDF Dataset for an update operation may be specified either in the operation string itself using the USING, USING NAMED, and/or WITH keywords, 
	 * or it may be specified via the using-graph-uri and using-named-graph-uri parameters.
	 * 
	 * It is an error to supply the using-graph-uri or using-named-graph-uri parameters when using this protocol to convey a SPARQL 1.1 Update request 
	 * that contains an operation that uses the USING, USING NAMED, or WITH clause.
	 * 
	 * A SPARQL Update processor should treat each occurrence of the using-graph-uri=g parameter in an update protocol operation 
	 * as if a USING <g> clause were included for every operation in the SPARQL 1.1 Update request. 
	 * 
	 * Similarly, a SPARQL Update processor should treat each occurrence of the using-named-graph-uri=g parameter in an update protocol operation 
	 * as if a USING NAMED <g> clause were included for every operation in the SPARQL 1.1 Update request.
	 * */
	protected Set<String> getGraphURIs(String sparql) throws QueryParseException {		
		UpdateRequest upd = new UpdateRequest();
		UpdateRequestSink sink = new UpdateRequestSink(upd);

		new ParserSPARQL11Update().parse(sink, sparql);

		for (Update op : upd.getOperations()) {
			if (op instanceof UpdateModify) {
				UpdateModify tmp = (UpdateModify) op;
				
				// WITH
				Node node = tmp.getWithIRI();
				if (node != null)
					if (node.isURI()) {
						rdfDataSet.add(node.getURI());
					}
				
				// USING
				for (Node n : tmp.getUsing()) {
					if (n.isURI())
						rdfDataSet.add(n.getURI());
					else if (n.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
				
				// USING NAMED
				for (Node n : tmp.getUsingNamed()) {
					if (n.isURI())
						rdfDataSet.add(n.getURI());
					else if (n.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
				
				// QUADS
				for (Quad q : tmp.getInsertQuads()) {
					Node n = q.getGraph();
					if (n.isURI())
						if (!n.getURI().equals(arqDefaultGraphNodeUri)) rdfDataSet.add(n.getURI());
					else if (n.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
				for (Quad q : tmp.getDeleteQuads()) {
					Node n = q.getGraph();
					if (n.isURI())
						if (!n.getURI().equals(arqDefaultGraphNodeUri)) rdfDataSet.add(n.getURI());
					else if (n.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
			} else if (op instanceof UpdateBinaryOp) {
				UpdateBinaryOp tmp = (UpdateBinaryOp) op;
				
				Node node = tmp.getDest().getGraph();
				
				// ADD, COPY, MOVE
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					//TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateCreate) {
				UpdateCreate tmp = (UpdateCreate) op;
				
				Node node = tmp.getGraph();
				
				// CREATE
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					//TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateData) {
				UpdateData tmp = (UpdateData) op;
				
				// UPDATE DATA
				for (Quad q : tmp.getQuads()) {
					Node node = q.getGraph();
					if (node.isURI())
						rdfDataSet.add(node.getURI());
					else if (node.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
			} else if (op instanceof UpdateDeleteWhere) {
				UpdateDeleteWhere tmp = (UpdateDeleteWhere) op;
				
				// UPDATE DELETE WHERE
				for (Quad q : tmp.getQuads()) {
					Node node = q.getGraph();
					if (node.isURI())
						rdfDataSet.add(node.getURI());
					else if (node.isVariable())
						//TODO: check
						rdfDataSet.add("*");
				}
			} else if (op instanceof UpdateDropClear) {
				UpdateDropClear tmp = (UpdateDropClear) op;
				
				Node node = tmp.getGraph();
				
				// DROP, CLEAR
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					//TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateLoad) {
				UpdateLoad tmp = (UpdateLoad) op;
				
				Node node = tmp.getDest();
				
				// LOAD
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					//TODO: check
					rdfDataSet.add("*");
			} 
		}

		return rdfDataSet;
	}
}
