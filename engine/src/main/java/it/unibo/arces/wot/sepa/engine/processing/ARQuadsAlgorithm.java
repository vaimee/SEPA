package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.engine.processing.lutt.FakeLUTT;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTT;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTTTriple;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.SPARQLParser;

public class ARQuadsAlgorithm {

	private static SPARQLParser sparqlParser = SPARQLParser.createParser(Syntax.syntaxSPARQL_11);
	
	public static InternalUpdateRequestWithQuads extractARQuads(InternalUpdateRequest update, QueryProcessor queryProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException {
		return new InternalUpdateRequestWithQuads(update.getSparql(), update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization(), new FakeLUTT());
	}

	public static InternalUpdateRequestWithQuads generateLUTTandInsertDelete(
					String originalSparql,
					UpdateResponse ret,
					Set<String> defaultGraphUri, 
					Set<String> namedGraphUri,
					ClientAuthorization auth
			) throws SPARQL11ProtocolException, SEPASparqlParsingException {
		
		//"jollyTriples" is not used jet, it would be used in case of default graph
		ArrayList<LUTTTriple> jollyTriples = new ArrayList<LUTTTriple>();
		
		HashMap<String, ArrayList<LUTTTriple>> quads = new 	HashMap<String, ArrayList<LUTTTriple>>();
		
		//exstract prefixs
		String sparqlOriginalLower = originalSparql.toLowerCase();
		String sparqlPrefix="";
		int cut1= sparqlOriginalLower.lastIndexOf("prefix");
		if(cut1>-1) {
			int cut2= sparqlOriginalLower.substring(cut1).indexOf(">");
			if(cut2>-1) {
				sparqlPrefix = originalSparql.substring(0,cut1+cut2+1)+"\n";
			}
		}
		
//		DEPRECATE
//		DEPRECATE
//		DEPRECATE
//		HashMap<String, String> resolvedPrefix= new HashMap<String, String>();
//		//this query is just for take prefix as "PrefixMapping" from JENA
//		//we will NOT run it
//		String sparqlUpdateToQuery = sparqlPrefix+"SELECT ?s WHERE {?s ?p ?o}";
//		Query jenaQuery = sparqlParser.parse(new Query(),sparqlUpdateToQuery);
//		PrefixMapping prefixs=jenaQuery.getPrefixMapping();
	
		String sparql = "";
		String rollback = "";
		HashMap<String,String> graph_triples = new HashMap<String,String>();
	
		if(ret.removedTuples.size() >0 ) {
			Iterator<Quad> removedIterator = ret.removedTuples.iterator();
			sparql="DELETE DATA{\n";
			rollback="INSERT DATA{";
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
				String newGraph="GRAPH <"+key+"> {"+graph_triples.get(key)+"}\n";
				sparql+=newGraph;
				rollback+=newGraph;
			}
			sparql+="};\n";
			rollback+="};\n";
			graph_triples = new HashMap<String,String>();
		}
		if(ret.updatedTuples.size() >0 ) {
			Iterator<Quad> updatedIterator = ret.updatedTuples.iterator();
			sparql+="INSERT DATA{\n";
			rollback+="DELETE DATA{\n";
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
				String newGraph="GRAPH <"+key+"> {"+graph_triples.get(key)+"}\n";
				sparql+=newGraph;
				rollback+=newGraph;
			}
			sparql+="};\n";
			rollback+="};\n";
		}
		LUTT hitter = new LUTT(jollyTriples,quads);
		return new InternalUpdateRequestWithQuads(sparqlPrefix+sparql,sparqlPrefix+rollback ,defaultGraphUri,namedGraphUri,auth, hitter);
		
	}
	
	public static InternalUpdateRequestWithQuads extractJenaARQuads(InternalUpdateRequest update, UpdateProcessor updateProcessor) throws SEPAProcessingException, SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
		
		//System.out.println("###########################update: \n"+update.getSparql());
		Response resp = updateProcessor.process(update);
		if(resp.isError()) {
			//is possible that exception "SEPAProcessingException" is not the right one
			throw new SEPAProcessingException(((ErrorResponse)resp).getError());
		}else {
			UpdateResponse ret = (UpdateResponse)resp;
			
		
		
			if(ret.removedTuples.size() <1 && ret.updatedTuples.size()<1 ) {
				//no quads, no SPU to active
				InternalUpdateRequestWithQuads ris = new InternalUpdateRequestWithQuads(
						"", 
						update.getDefaultGraphUri(), 
						update.getNamedGraphUri(), 
						update.getClientAuthorization(), 
						new LUTT()
					);
				ris.setResponseNothingToDo(ret);
				return ris;
			}
			
		
			return generateLUTTandInsertDelete(update.getSparql(),ret,update.getDefaultGraphUri(), update.getNamedGraphUri(), update.getClientAuthorization());
			
		}
		
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
			String datatype = n.getLiteralDatatype().getURI();
			if(datatype!=null && datatype.length()>0) {
					return "'''"+n.getLiteralLexicalForm()+"'''^^<"+datatype+">";
			}
			String justDataType =n.getLiteralDatatypeURI();
			String result = n.toString();
			int index = result.indexOf(justDataType);
			if(index>-1) {
				result=result.substring(0, index)+"<"+result.substring(index)+">";
				return result;
			}else {
				return result;
			}
			//			String temp =n.getLiteralLexicalForm();
			//			if(!temp.startsWith("\"") && !temp.startsWith("'")&& !temp.startsWith("'''")) {
			//				return "'''"+n.getLiteralLexicalForm()+"'''";
			//			}
			//			return n.getLiteralLexicalForm();	
		}else if(n.isBlank()) {
			return n.getBlankNodeLabel(); 		//need to be tested
		}
		return n.toString(); 					//this can be a problem
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
