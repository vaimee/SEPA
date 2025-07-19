package com.vaimee.sepa.engine.scheduling;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.Prologue;
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
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateRequest;

import com.vaimee.sepa.api.commons.exceptions.SEPASparqlParsingException;
import com.vaimee.sepa.logging.Logging;

public class JenaSparqlParsing {
	
	protected final static String arqDefaultGraphNodeUri = "urn:x-arq:DefaultGraphNode";
	
	public JenaSparqlParsing() {
		ARQ.init();
	}
	
	/*
	 * 2.2.3 Specifying an RDF Dataset
	 * 
	 * SPARQL Update requests are executed against a Graph Store, a mutable
	 * container of RDF graphs managed by a SPARQL service.
	 * 
	 * The WHERE clause of a SPARQL update DELETE/INSERT operation [UPDATE] matches
	 * against data in an RDF Dataset, which is a subset of the Graph Store.
	 * 
	 * The RDF Dataset for an update operation may be specified either in the
	 * operation string itself using the USING, USING NAMED, and/or WITH keywords,
	 * or it may be specified via the using-graph-uri and using-named-graph-uri
	 * parameters.
	 * 
	 * It is an error to supply the using-graph-uri or using-named-graph-uri
	 * parameters when using this protocol to convey a SPARQL 1.1 Update request
	 * that contains an operation that uses the USING, USING NAMED, or WITH clause.
	 * 
	 * A SPARQL Update processor should treat each occurrence of the
	 * using-graph-uri=g parameter in an update protocol operation as if a USING <g>
	 * clause were included for every operation in the SPARQL 1.1 Update request.
	 * 
	 * Similarly, a SPARQL Update processor should treat each occurrence of the
	 * using-named-graph-uri=g parameter in an update protocol operation as if a
	 * USING NAMED <g> clause were included for every operation in the SPARQL 1.1
	 * Update request.
	 */

	public Set<String> getUpdateGraphURIs(String sparql) throws SEPASparqlParsingException {
		UpdateRequest upd = new UpdateRequest(Context.emptyContext());

		UpdateRequestSink sink = new UpdateRequestSink(upd);

		Set<String> rdfDataSet = new HashSet<String>();
		
		try {
			new ParserSPARQL11Update().parse(sink, new Prologue(),sparql);
		} catch (Exception e) {
			Logging.getLogger().error("SPARQL: "+sparql+" MESSAGE: "+e.getMessage());
			throw new SEPASparqlParsingException("SPARQL: "+sparql+" MESSAGE: "+e.getMessage());
//			Logging.getLogger().warn("Parsing exception "+e.getMessage());
//			rdfDataSet.add("*");
//			return rdfDataSet;
		}

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
						// TODO: check
						rdfDataSet.add("*");
				}

				// USING NAMED
				for (Node n : tmp.getUsingNamed()) {
					if (n.isURI())
						rdfDataSet.add(n.getURI());
					else if (n.isVariable())
						// TODO: check
						rdfDataSet.add("*");
				}

				// QUADS
				for (Quad q : tmp.getInsertQuads()) {
					Node n = q.getGraph();
					if (n.isURI())
						if (!n.getURI().equals(arqDefaultGraphNodeUri))
							rdfDataSet.add(n.getURI());
						else if (n.isVariable())
							// TODO: check
							rdfDataSet.add("*");
				}
				for (Quad q : tmp.getDeleteQuads()) {
					Node n = q.getGraph();
					if (n.isURI())
						if (!n.getURI().equals(arqDefaultGraphNodeUri))
							rdfDataSet.add(n.getURI());
						else if (n.isVariable())
							// TODO: check
							rdfDataSet.add("*");
				}
			} else if (op instanceof UpdateBinaryOp) {
				UpdateBinaryOp tmp = (UpdateBinaryOp) op;

				Node node = tmp.getDest().getGraph();

				// ADD, COPY, MOVE
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					// TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateCreate) {
				UpdateCreate tmp = (UpdateCreate) op;

				Node node = tmp.getGraph();

				// CREATE
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					// TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateData) {
				UpdateData tmp = (UpdateData) op;

				// UPDATE DATA
				for (Quad q : tmp.getQuads()) {
					Node node = q.getGraph();
					if (node.isURI())
						rdfDataSet.add(node.getURI());
					else if (node.isVariable())
						// TODO: check
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
						// TODO: check
						rdfDataSet.add("*");
				}
			} else if (op instanceof UpdateDropClear) {
				UpdateDropClear tmp = (UpdateDropClear) op;

				Node node = tmp.getGraph();

				// DROP, CLEAR
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					// TODO: check
					rdfDataSet.add("*");
			} else if (op instanceof UpdateLoad) {
				UpdateLoad tmp = (UpdateLoad) op;

				Node node = tmp.getDest();

				// LOAD
				if (node.isURI())
					rdfDataSet.add(node.getURI());
				else if (node.isVariable())
					// TODO: check
					rdfDataSet.add("*");
			}
		}

		return rdfDataSet;
	}
	
	public Set<String> getQueryGraphURIs(String sparql) throws SEPASparqlParsingException {
		Set<String> ret = new HashSet<>();

		if (sparql == null)
			return ret;

		Query q = null;
		Logging.getLogger().trace("Parsing query: " + sparql);
		try{
			q = QueryFactory.create(sparql);
		}
		catch(Exception e) {
//			Logging.getLogger().error("FAILED TO CREATE QUERY WITH JENA "+e.getMessage()+" Query "+sparql);
//			ret.add("*");
//			return ret;
			Logging.getLogger().error(e.getMessage());
			throw new SEPASparqlParsingException(e.getMessage());
		}

		Logging.getLogger().trace("Get dataset descriptiors");
		if (q.hasDatasetDescription()) {
			Logging.getLogger().trace("Get default graph URIs");
			for (String gr : q.getDatasetDescription().getDefaultGraphURIs()) {
				ret.add(gr);
			}
			Logging.getLogger().trace("Get named graph URIs");
			for (String gr : q.getDatasetDescription().getNamedGraphURIs()) {
				ret.add(gr);
			}
		}

		Logging.getLogger().trace("Get graph URIs");
		List<String> graphs = q.getGraphURIs();
		Logging.getLogger().trace("Get named graph URIs");
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

		Logging.getLogger().trace("Extract graphs " + e);
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
