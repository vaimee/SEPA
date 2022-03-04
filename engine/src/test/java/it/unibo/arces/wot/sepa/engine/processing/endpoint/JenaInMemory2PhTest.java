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
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JenaInMemory2PhTest {
	
	private static JenaInMemory2PhEndpoint inMemEndPoint;

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
	public void TEST_01_INSERT_DATA() throws SEPASecurityException {
		String sparqlUpdate = 
					"INSERT DATA  {  \r\n"
					+ "	GRAPH <http://g1> { \r\n"
					+ "		<http://s1> <http://p1> <http://o1> .\r\n"
					+ "		<http://s2> <http://p2> <http://o3> .\r\n"
					+ "		<http://s2> <http://p3> <http://o3> .\r\n"
					+ "		<http://s3> <http://p3> <http://o3> .\r\n"
					+ "		<http://s3> <http://p1> <http://o3> .\r\n"
					+ "	}\r\n"
					+ "	GRAPH <http://g2> { \r\n"
					+ "		<http://s1> <http://p1> <http://o1> .\r\n"
					+ "		<http://s2> <http://p2> <http://o3> .\r\n"
					+ "		<http://s2> <http://p3> <http://o3> .\r\n"
					+ "		<http://s3> <http://p3> <http://o3> .\r\n"
					+ "		<http://s3> <http://p1> <http://o3> .\r\n"
					+ "		<http://s3> <http://p1> <http://o9> .\r\n"
					+ "		<http://s3> <http://p1> <http://o10> .\r\n"
					+ "	}\r\n"
					+ "}	";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expected.add(new TempQuadForTest("http://g1","http://s1","http://p1","http://o1"));
		expected.add(new TempQuadForTest("http://g2","http://s1","http://p1","http://o1"));
		expected.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o3"));
		expected.add(new TempQuadForTest("http://g2","http://s2","http://p2","http://o3"));
		expected.add(new TempQuadForTest("http://g1","http://s2","http://p3","http://o3"));
		expected.add(new TempQuadForTest("http://g2","http://s2","http://p3","http://o3"));
		expected.add(new TempQuadForTest("http://g1","http://s3","http://p1","http://o3"));
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o3"));
		expected.add(new TempQuadForTest("http://g1","http://s3","http://p3","http://o3"));
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p3","http://o3"));
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o9"));
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o10"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.updatedTuples,expected,"01"));
		assertTrue(res.removedTuples.size()==0);
		
	}
	

	@Test
	public void Test_02_DELETE_DATA() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE DATA { \r\n"
					+ "	GRAPH  <http://g2> { \r\n"
					+ "		<http://s3> <http://p1> <http://o10>\r\n"
					+ "	}\r\n"
					+ "}\r\n"
					+ "";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o10"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expected,"02"));
		assertTrue(res.updatedTuples.size()==0);

	}
	
	@Test
	public void Test_03_DELETE_WHERE() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE WHERE {\r\n"
					+ "	GRAPH ?g { \r\n"
					+ "		?s ?p <http://o9> .\r\n"
					+ "	} \r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expected.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o9"));
		assertTrue(quadsSetCompare(res.removedTuples,expected,"03"));
		assertTrue(res.updatedTuples.size()==0);
	}
	
	@Test
	public void Test_04_UPDATE_MODIFY_noWhere() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE { \r\n"
					+ "	GRAPH <http://g1>  {\r\n"
					+ "		<http://s2> <http://p2> <http://o3>\r\n"
					+ "	} \r\n"
					+ "} INSERT {\r\n"
					+ "	GRAPH <http://g1>  {\r\n"
					+ "		<http://s2> <http://p2> <http://o2>\r\n"
					+ "	} \r\n"
					+ "} WHERE {}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o3"));
		expectedAdded.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o2"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"04.removed"));
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"04.added"));

	}
	
	@Test
	public void Test_05_UPDATE_MODIFY_where() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE {  \r\n"
					+ "		GRAPH ?g  {\r\n"
					+ "			?s ?p ?o\r\n"
					+ "		} \r\n"
					+ "} INSERT {  \r\n"
					+ "		GRAPH ?g {			\r\n"
					+ "			?s <http://p2> <http://o10>\r\n"
					+ "		} \r\n"
					+ "} WHERE {\r\n"
					+ "        GRAPH ?g {\r\n"
					+ "            ?s ?p ?o .\r\n"
					+ "            ?s <http://p2> <http://o2>\r\n"
					+ "        }\r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s2","http://p3","http://o3"));
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o2"));
		expectedAdded.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o10"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"05.removed"));
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"05.added"));

	}
	
	@Test
	public void Test_06_UPDATE_MODIFY_reverseWhereANDfilter() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE {  \r\n"
					+ "		GRAPH ?g  {\r\n"
					+ "			?s ?p ?o\r\n"
					+ "		} \r\n"
					+ "} INSERT {  \r\n"
					+ "		GRAPH ?g {			\r\n"
					+ "			?s <http://p2> <http://o10>\r\n"
					+ "		} \r\n"
					+ "} WHERE {\r\n"
					+ "        GRAPH ?g {\r\n"
					+ "            ?s ?p <http://o3> .\r\n"
					+ "            ?s ?p ?o \r\n"
					+ "        }\r\n"
					+ "        FILTER(?g = <http://g2>)\r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s2","http://p2","http://o3"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s2","http://p3","http://o3"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s3","http://p1","http://o3"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s3","http://p3","http://o3"));

		expectedAdded.add(new TempQuadForTest("http://g2","http://s2","http://p2","http://o10"));
		expectedAdded.add(new TempQuadForTest("http://g2","http://s3","http://p2","http://o10"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"06.removed"));
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"06.added"));

	}
	
	@Test
	public void Test_07_UPDATE_MODIFY_valuesMovBetweenGraph() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE {  \r\n"
					+ "		GRAPH ?g  {\r\n"
					+ "			?s ?p ?o\r\n"
					+ "		} \r\n"
					+ "} INSERT {  \r\n"
					+ "		GRAPH <http://g2> {			\r\n"
					+ "			?s ?p ?o\r\n"
					+ "		} \r\n"
					+ "} WHERE {\r\n"
					+ "        GRAPH ?g {\r\n"
					+ "            ?s ?p ?o \r\n"
					+ "        }\r\n"
					+ "	VALUES (?s ?p ?o) {\r\n"
					+ "		  (<http://s3><http://p3><http://o3>)\r\n"
					+ "		  (<http://x0><http://p1><http://o3>)\r\n"
					+ "	}\r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s3","http://p3","http://o3"));

		expectedAdded.add(new TempQuadForTest("http://g2","http://s3","http://p3","http://o3"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"07.removed"));
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"07.added"));

	}
	
	@Test
	public void Test_08_UPDATE_MODIFY_with() throws SEPASecurityException {
		String sparqlUpdate = 
					"WITH <http://g2>\r\n"
					+ "DELETE {  \r\n"
					+ "	?s ?p ?o\r\n"
					+ "} INSERT {  	\r\n"
					+ "	?s <http://p0> <http://o0>\r\n"
					+ "} WHERE {\r\n"
					+ "    	?s ?p ?o\r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s1","http://p1","http://o1"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s2","http://p2","http://o10"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s3","http://p2","http://o10"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s3","http://p3","http://o3"));

		expectedAdded.add(new TempQuadForTest("http://g2","http://s1","http://p0","http://o0"));
		expectedAdded.add(new TempQuadForTest("http://g2","http://s2","http://p0","http://o0"));
		expectedAdded.add(new TempQuadForTest("http://g2","http://s3","http://p0","http://o0"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"08.removed"));
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"08.added"));

	}
	
	@Test
	public void Test_09_INSERT_WHERE_bind() throws SEPASecurityException {
		String sparqlUpdate = 
					"INSERT {   \r\n"
					+ "	GRAPH <http://g2> { \r\n"
					+ "		<http://s4><http://p4><http://o4>\r\n"
					+ "	}\r\n"
					+ "	GRAPH <http://g3> { \r\n"
					+ "		?s <http://p> <http://s2> .\r\n"
					+ "		?s <http://p> <http://s3> .\r\n"
					+ "	}\r\n"
					+ "}WHERE {\r\n"
					+ "\r\n"
					+ "    BIND( <http://s> AS ?s)\r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedAdded.add(new TempQuadForTest("http://g2","http://s4","http://p4","http://o4"));
		expectedAdded.add(new TempQuadForTest("http://g3","http://s","http://p","http://s2"));
		expectedAdded.add(new TempQuadForTest("http://g3","http://s","http://p","http://s3"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"09.added"));
		assertTrue(res.removedTuples.size()==0);

	}
	
	
	@Test
	public void Test_10_UPDATE_MODIFY_crosGgraphANDinnestedQuery() throws SEPASecurityException {
		String sparqlUpdate = 
					"DELETE {\r\n"
					+ "    GRAPH ?g {\r\n"
					+ "        ?sbj ?p ?o.\r\n"
					+ "    }\r\n"
					+ "}INSERT  {  \r\n"
					+ "    GRAPH <http://g3> {\r\n"
					+ "        <http://s10> ?g ?sbj.\r\n"
					+ "    }\r\n"
					+ "}WHERE {\r\n"
					+ "    GRAPH ?g {\r\n"
					+ "        ?sbj ?p ?o.\r\n"
					+ "    }\r\n"
					+ "    {\r\n"
					+ "        SELECT ?sbj WHERE {\r\n"
					+ "            GRAPH <http://g3> {?s ?p ?sbj}\r\n"
					+ "        } GROUP BY ?sbj\r\n"
					+ "    } \r\n"
					+ "}";
		//printQueryAll();
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedDeleted = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s2","http://p2","http://o10"));
		expectedDeleted.add(new TempQuadForTest("http://g1","http://s3","http://p1","http://o3"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s2","http://p0","http://o0"));
		expectedDeleted.add(new TempQuadForTest("http://g2","http://s3","http://p0","http://o0"));

		expectedAdded.add(new TempQuadForTest("http://g3","http://s10","http://g1","http://s2"));
		expectedAdded.add(new TempQuadForTest("http://g3","http://s10","http://g1","http://s3"));
		expectedAdded.add(new TempQuadForTest("http://g3","http://s10","http://g2","http://s2"));
		expectedAdded.add(new TempQuadForTest("http://g3","http://s10","http://g2","http://s3"));
		//printQueryAll();
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"10.added"));
		assertTrue(quadsSetCompare(res.removedTuples,expectedDeleted,"10.removed"));
	}
	
	@Test
	public void Test_11_INSERT_WHERE_crosGgraphANDinnestedQuery() throws SEPASecurityException {
		String sparqlUpdate = 
					"INSERT  {  \r\n"
					+ "    GRAPH <http://counters> {\r\n"
					+ "        ?g ?pX ?c.\r\n"
					+ "    }\r\n"
					+ "}WHERE {\r\n"
					+ "	GRAPH <http://g1> { \r\n"
					+ "		?sX ?pX ?oX .\r\n"
					+ "	}\r\n"
					+ "    {\r\n"
					+ "        SELECT ?g (count(distinct ?s) as ?c)\r\n"
					+ "        WHERE {\r\n"
					+ "            GRAPH ?g {\r\n"
					+ "                ?s ?p ?o.\r\n"
					+ "            }\r\n"
					+ "        }GROUP BY ?g\r\n"
					+ "    } \r\n"
					+ "}";
		UpdateResponse res= (UpdateResponse)inMemEndPoint.update(sparqlUpdate);
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expectedAdded.add(new TempQuadForTest("http://counters","http://g1","http://p1","1"));
		expectedAdded.add(new TempQuadForTest("http://counters","http://g2","http://p1","2"));
		expectedAdded.add(new TempQuadForTest("http://counters","http://g3","http://p1","2"));
		//printQueryAll();
		assertTrue(res.removedTuples.size()==0);
		assertTrue(quadsSetCompare(res.updatedTuples,expectedAdded,"11.added"));

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



