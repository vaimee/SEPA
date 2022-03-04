package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.sparql.core.Quad;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JenaInMemory2PhChatTest {
	
	private static JenaInMemory2PhEndpoint inMemEndPoint;
	private static String prefixs = "PREFIX schema:<http://schema.org/> " +
			 "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			 "PREFIX chat:<http://wot.arces.unibo.it/chat#>\n";
	@BeforeClass
	public static void init() throws SEPASecurityException {
		//using "primary"	-> 	JenaInMemory2PhEndpoint(false)
		//or "alternative" 	->	JenaInMemory2PhEndpoint(true)
		//dataset are the same for the scope
		//of that test, we will use the "primary"
		inMemEndPoint=new JenaInMemory2PhEndpoint(false);
		//clean dataset
		String delete_all = "DELETE WHERE { GRAPH ?g {?s ?p ?o}}";
		inMemEndPoint.update(delete_all);
			
		//#############Reminder: 
		//	The update complexity grows as the update number (N) increases.
		//	The updates need to be called following the order, following the number (N)
	}



	@Test
	public void TEST_01_SEND() throws SEPASecurityException {
		String sparqlUpdate = prefixs+
					"WITH <http://wot.arces.unibo.it/chat/room_default> INSERT {\r\n"
					+ "						<http://prova> rdf:type schema:Message ;\r\n"
					+ "						schema:text \"Ciao!\" ;\r\n"
					+ "						schema:sender chat:IamASender ;\r\n"
					+ "						schema:toRecipient chat:IamAReceiver.\r\n"
					+ "					} WHERE {\r\n"
					+ "						GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
					+ "							chat:IamASender rdf:type schema:Person .\r\n"
					+ "							chat:IamAReceiver rdf:type schema:Person \r\n"
					+ "						}\r\n"
					+ "					}";
		System.out.println("sparqlUpdate");
		printQueryAll();
		Response res= inMemEndPoint.update(sparqlUpdate);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateRes = (UpdateResponse)res;
			printQueryAll();
			Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
//			expected.add(new TempQuadForTest("http://g1","http://s1","http://p1","http://o1"));
		
//			assertTrue(quadsSetCompare(res.updatedTuples,expected,"01"));
//			assertTrue(res.removedTuples.size()==0);
			
		}

	}
	

	
	private boolean quadsSetCompare(Set<Quad> found,Set<TempQuadForTest> expected, String testName) {
		if(found.size()==expected.size()) {
			for (TempQuadForTest tempQuadForTest : expected) {
				boolean pass=false;
				for (Quad realQuad : found) {
					if(realQuad.getGraph().getURI().compareTo(tempQuadForTest.getGraph())==0){
						if(realQuad.getSubject().getURI().compareTo(tempQuadForTest.getSubject())==0) {
							if(realQuad.getPredicate().getURI().compareTo(tempQuadForTest.getPredicate())==0) {
								if(realQuad.getObject().isURI()) {
									if(realQuad.getObject().getURI().compareTo(tempQuadForTest.getObject())==0) {
										pass=true;
										break;
									}
								}else {
									String temp = realQuad.getObject().getLiteralLexicalForm();
									if(realQuad.getObject().getLiteralLexicalForm().compareTo(tempQuadForTest.getObject())==0) {
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

	private void printQueryAll() {
		QueryResponse qr= (QueryResponse)inMemEndPoint.query("SELECT ?g ?s ?p ?o WHERE { GRAPH  ?g{ ?s ?p ?o}}");
		System.out.println("#############Query all: \n"+qr.toString());
	}
}



