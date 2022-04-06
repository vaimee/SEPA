package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTT;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTTTriple;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

public class ARQuadsAlgorithm {

	public static InternalUpdateRequestWithQuads extractARQuads(InternalUpdateRequest update, QueryProcessor queryProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException {
		return new InternalUpdateRequestWithQuads(update.getSparql(), update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization(), null);
	}

	public static InternalUpdateRequestWithQuads extractJenaARQuads(InternalUpdateRequest update, UpdateProcessor updateProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
	
		UpdateResponse ret = (UpdateResponse)updateProcessor.process(update);
		String sparql = "";
		HashMap<String,String> graph_triples = new HashMap<String,String>();
		//"jollyTriples" is not used jet, it would be used in case of default graph
		ArrayList<LUTTTriple> jollyTriples = new ArrayList<LUTTTriple>();
		HashMap<String, ArrayList<LUTTTriple>> quads = new 	HashMap<String, ArrayList<LUTTTriple>>();
		if(ret.removedTuples.size() >0 ) {
			Iterator<Quad> removedIterator = ret.removedTuples.iterator();
			sparql="DELETE DATA{\n";
			while(removedIterator.hasNext()) {
				Quad q = removedIterator.next();
				String graph = q.getGraph().getURI();
				Triple t = q.asTriple();
				String triples = tripleToStringForSparql(t);
				//---- for update delete-insert
				if(graph_triples.containsKey(graph)) {
					graph_triples.put(graph, graph_triples.get(graph) +"\n"+ triples);
				}else {
					graph_triples.put(graph,triples);
				}
				//---- for LUTT
				if(quads.containsKey(graph)) {
					quads.get(graph).add(tripleToLUTTTriple(t));
				}else {
					ArrayList<LUTTTriple> lutttriple = new ArrayList<LUTTTriple>();
					lutttriple.add(tripleToLUTTTriple(t));
					quads.put(graph,lutttriple);
				}
			}
			for (String key : graph_triples.keySet()) {
				sparql+="GRAPH <"+key+"> {"+graph_triples.get(key)+"}\n";
			}
			sparql+="};\n";
			graph_triples = new HashMap<String,String>();
		}
		if(ret.updatedTuples.size() >0 ) {
			Iterator<Quad> updatedIterator = ret.updatedTuples.iterator();
			sparql+="INSERT DATA{\n";
			while(updatedIterator.hasNext()) {
				Quad q = updatedIterator.next();
				String graph = q.getGraph().getURI();
				Triple t = q.asTriple();
				String triples = tripleToStringForSparql(t);
				//---- for update delete-insert
				if(graph_triples.containsKey(graph)) {
					graph_triples.put(graph, graph_triples.get(graph) +"\n"+ triples);
				}else {
					graph_triples.put(graph,triples);
				}
				//---- for LUTT
				if(quads.containsKey(graph)) {
					quads.get(graph).add(tripleToLUTTTriple(t));
				}else {
					ArrayList<LUTTTriple> lutttriple = new ArrayList<LUTTTriple>();
					lutttriple.add(tripleToLUTTTriple(t));
					quads.put(graph,lutttriple);
				}
			}
			for (String key : graph_triples.keySet()) {
				sparql+="GRAPH <"+key+"> {"+graph_triples.get(key)+"}\n";
			}
			sparql+="};\n";
		}
		LUTT hitter = new LUTT(jollyTriples,quads);
		return new InternalUpdateRequestWithQuads(sparql, update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization(), hitter);
	}
	
	private static String tripleToStringForSparql(Triple t) {
			return nodeToStringForSparql(t.getSubject())+ 
					" " + nodeToStringForSparql(t.getPredicate()) + 
					" " + nodeToStringForSparql(t.getObject()) + ".";
	}
	
	private static LUTTTriple tripleToLUTTTriple(Triple t) {
		
		return new LUTTTriple(nodeToStringForLUTTTriple(t.getSubject()), 
					nodeToStringForLUTTTriple(t.getPredicate()), 
					nodeToStringForLUTTTriple(t.getObject()));
	}
	
	private static String nodeToStringForSparql(Node n) {
		if(n.isURI()) {
			return "<"+n.getURI()+">"; 			
		}else if(n.isLiteral()) {
			return n.getLiteralLexicalForm();	
		}else if(n.isBlank()) {
			return n.getBlankNodeLabel(); 		//need to be tested
		}
		return n.toString(); 					//this can be problematic
	}
	
	private static String nodeToStringForLUTTTriple(Node n) {
		if(n.isURI()) {
			return n.getURI(); 			
		}else if(n.isLiteral()) {
			return n.getLiteralLexicalForm();	
		}else if(n.isBlank()) {
			return n.getBlankNodeLabel(); 		//need to be tested
		}
		return n.toString(); 					//this can be problematic
	}
}
