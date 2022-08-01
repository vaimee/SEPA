package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import java.util.Set;

import org.apache.jena.sparql.core.Quad;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;

public class TestUtils {

	private static final UpdateHTTPMethod updateMethod = UpdateHTTPMethod.POST;
	private static final QueryHTTPMethod queryMethod = QueryHTTPMethod.POST;
	private static final String scheme = "http";
	private static final String host = "localhost";
	private static final int port = 8000;
	private static final String updatePath = "/sparql";
	private static final String queryPath = "/sparql";
	
	

	public static boolean quadsSetCompare(Set<Quad> found,Set<TempQuadForTest> expected, String testName) {
		if(found.size()==expected.size()) {
			for (TempQuadForTest tempQuadForTest : expected) {
				boolean pass=false;
				for (Quad realQuad : found) {
					if(realQuad.getGraph().getURI().compareTo(tempQuadForTest.getGraph())==0){
						if(tempQuadForTest.getSubject()==null || realQuad.getSubject().getURI().compareTo(tempQuadForTest.getSubject())==0) {
							if(tempQuadForTest.getPredicate()==null || realQuad.getPredicate().getURI().compareTo(tempQuadForTest.getPredicate())==0) {
								if(realQuad.getObject().isURI()) {
									if(
											tempQuadForTest.getObject()==null ||
											realQuad.getObject().getURI().compareTo(tempQuadForTest.getObject())==0
									) {
										pass=true;
										break;
									}
								}else {
									String temp = realQuad.getObject().getLiteralLexicalForm();
//									System.out.println("----->"+temp);
									if(
											tempQuadForTest.getObject()==null ||
											temp.compareTo(tempQuadForTest.getObject())==0
									) {
										pass=true;
										break;
									}
								}
							}
						}
					}
				}
				if(!pass) {
					System.out.println("TEST["+testName+"] not pass: miss-quad: "+tempQuadForTest.toString());
					return false;
				}
			}
			return true;
		}
		System.out.println("TEST["+testName+"] not pass set size is "+found.size() + " and was expected "+ expected.size());
		return false;
	}

	public static void printQueryAll(SjenarEndpoint inMemEndPoint) {
		QueryResponse qr= (QueryResponse)inMemEndPoint.query(generateQuery("SELECT ?g ?s ?p ?o WHERE { GRAPH  ?g{ ?s ?p ?o}}"));
		System.out.println("#############Query all: \n"+qr.toString());
	}
	
	public static UpdateRequest generateUpdate(String sparql) {
		 return new UpdateRequest(
						updateMethod,
						scheme,
						host,
						port,
						updatePath,
						sparql,
						null, 
						null,
						null
					);
	}
	
	public static QueryRequest generateQuery(String sparql) {
		 return new QueryRequest(
				 		queryMethod,
				 		scheme,
						host,
						port,
						queryPath,
						sparql,
						null, 
						null,
						null,
						60000,
						0
					);
	}
	

}