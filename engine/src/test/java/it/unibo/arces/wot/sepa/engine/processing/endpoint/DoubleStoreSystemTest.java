package it.unibo.arces.wot.sepa.engine.processing.endpoint;


import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.sparql.core.Quad;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.processing.ARQuadsAlgorithm;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.ar.UpdateResponseWithAR;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DoubleStoreSystemTest {

	private static SjenarEndpointDoubleStore firstStore;
	private static SjenarEndpointDoubleStore secondStore;
	
	@BeforeClass
	public static void init() throws SEPASecurityException {
		firstStore=new SjenarEndpointDoubleStore(true);
		secondStore=new SjenarEndpointDoubleStore(false);
		//clean datasets
		clean();
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
		UpdateResponseWithAR res1= (UpdateResponseWithAR)firstStore.update(TestUtils.generateUpdate(sparqlUpdate));
		UpdateResponseWithAR res2= (UpdateResponseWithAR)secondStore.update(TestUtils.generateUpdate(sparqlUpdate));
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
		assertTrue(TestUtils.quadsSetCompare(res1.updatedTuples,expected,"01.store1"));
		assertTrue(res1.removedTuples.size()==0);
		assertTrue(TestUtils.quadsSetCompare(res2.updatedTuples,expected,"01.store2"));
		assertTrue(res2.removedTuples.size()==0);
	}

	@Test
	public void TEST_02_INSERT_DATA_DATATYPE() throws SEPASecurityException {
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s5> <http://p5> \"true\" .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:boolean .\r\n"
				+ "	}\r\n"
				+ "}	";
		UpdateResponseWithAR res1= (UpdateResponseWithAR)firstStore.update(TestUtils.generateUpdate(sparqlUpdate));
		UpdateResponseWithAR res2= (UpdateResponseWithAR)secondStore.update(TestUtils.generateUpdate(sparqlUpdate));
		Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expected.add(new TempQuadForTest("http://g1","http://s5","http://p5","true"));
		expected.add(new TempQuadForTest("http://g2","http://s5","http://p5","true"));
		//printQueryAll();
		assertTrue(TestUtils.quadsSetCompare(res1.updatedTuples,expected,"02.store1"));
		assertTrue(res1.removedTuples.size()==0);
		assertTrue(TestUtils.quadsSetCompare(res2.updatedTuples,expected,"02.store2"));
		assertTrue(res2.removedTuples.size()==0);
	}

	@Test
	public void TEST_03_QUERY_DATATYPE_BOOLEAN() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:boolean .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1);
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_04_QUERY_DATATYPE_STR() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:string .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1);
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_05_QUERY_DATATYPE_DEFAULT() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\" .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1);
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_06_QUERY_DATATYPE_INTEGER() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:integer .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==0);
		assertTrue(res2.getBindingsResults().getBindings().size()==0);
	}

	@Test
	public void TEST_07_CLEAN() throws SEPASecurityException {
		clean();
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		?s ?p ?o .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==0);
		assertTrue(res2.getBindingsResults().getBindings().size()==0);
	}

	@Test
	public void TEST_08_INSERT_DATA_2STORE() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
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

		UpdateResponseWithAR res1= (UpdateResponseWithAR)firstStore.update(TestUtils.generateUpdate(sparqlUpdate));
		assertTrue(TestUtils.quadsSetCompare(res1.updatedTuples,expected,"08.store1"));
		assertTrue(res1.removedTuples.size()==0);
		

		InternalUpdateRequestWithQuads req2ph= ARQuadsAlgorithm.generateLUTTandInsertDelete(sparqlUpdate, res1, null, null, null);
		assertTrue(req2ph.getResponseNothingToDo()==null && req2ph.getSparql().length()>0);
		
		UpdateResponseWithAR res2= (UpdateResponseWithAR)secondStore.update(TestUtils.generateUpdate(req2ph.getSparql()));
		
		assertTrue(TestUtils.quadsSetCompare(res2.updatedTuples,expected,"08.store2"));
		assertTrue(res2.removedTuples.size()==0);
	}
	
	@Test
	public void TEST_09_INSERT_DATA_2STORE_DATATYPE() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s5> <http://p5> \"true\" .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:boolean .\r\n"
				+ "	}\r\n"
				+ "}	";
		
		Set<TempQuadForTest> expected = new HashSet<TempQuadForTest>();
		//order TempQuadForTest args: graph, subject, predicate, object
		expected.add(new TempQuadForTest("http://g1","http://s5","http://p5","true"));
		expected.add(new TempQuadForTest("http://g2","http://s5","http://p5","true"));

		UpdateResponseWithAR res1= (UpdateResponseWithAR)firstStore.update(TestUtils.generateUpdate(sparqlUpdate));
		assertTrue(TestUtils.quadsSetCompare(res1.updatedTuples,expected,"09.store1"));
		assertTrue(res1.removedTuples.size()==0);
		

		InternalUpdateRequestWithQuads req2ph= ARQuadsAlgorithm.generateLUTTandInsertDelete(sparqlUpdate, res1, null, null, null);
		assertTrue(req2ph.getResponseNothingToDo()==null && req2ph.getSparql().length()>0);
		System.out.println("->TEST_09_INSERT_DATA_2STORE_DATATYPE\n"+req2ph.getSparql());
		UpdateResponseWithAR res2= (UpdateResponseWithAR)secondStore.update(TestUtils.generateUpdate(req2ph.getSparql()));
		
		assertTrue(TestUtils.quadsSetCompare(res2.updatedTuples,expected,"09.store2"));
		assertTrue(res2.removedTuples.size()==0);
	}
	
	@Test
	public void TEST_10_QUERY_DATATYPE_BOOLEAN() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:boolean .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1);
		//WIP
		System.out.println("TEST_10.res2: "+ res2.getBindingsResults().getBindings().toString());
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_11_QUERY_DATATYPE_STR() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:string .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1);
		//WIP
		System.out.println("TEST_11.res2: "+ res2.getBindingsResults().getBindings().toString());
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_12_QUERY_DATATYPE_DEFAULT() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\" .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==1); 
		//WIP
		System.out.println("TEST_12.res2: "+ res2.getBindingsResults().getBindings().toString());
		assertTrue(res2.getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_13_QUERY_DATATYPE_INTEGER() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g  {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> \"true\"^^xsd:integer .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==0);
		//System.out.println("TEST_13.res2: "+ res2.getBindingsResults().getBindings().toString());//OK
		assertTrue(res2.getBindingsResults().getBindings().size()==0);
	}
	
	@Test
	public void TEST_14_QUERY_DATATYPE_VAR() throws SEPASecurityException {
		String sparqlQuery= "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"SELECT ?g ?p {  \r\n"
				+ "	GRAPH ?g { \r\n"
				+ "		<http://s5> <http://p5> ?p .\r\n"
				+ "	}\r\n"
				+ "}	";
		QueryResponse res1= (QueryResponse)firstStore.query(TestUtils.generateQuery(sparqlQuery));
		QueryResponse res2= (QueryResponse)secondStore.query(TestUtils.generateQuery(sparqlQuery));
		assertTrue(!res1.isError());
		assertTrue(!res2.isError());
		assertTrue(res1.getBindingsResults().getBindings().size()==2);
		//System.out.println("TEST_14.res2: "+ res2.getBindingsResults().getBindings().toString()); //ok
		assertTrue(res2.getBindingsResults().getBindings().size()==2);
	}
	
	private static void clean() {
		String delete_all = "DELETE WHERE { GRAPH ?g {?s ?p ?o}}";
		firstStore.update(TestUtils.generateUpdate(delete_all));
		secondStore.update(TestUtils.generateUpdate(delete_all));
	}


}



