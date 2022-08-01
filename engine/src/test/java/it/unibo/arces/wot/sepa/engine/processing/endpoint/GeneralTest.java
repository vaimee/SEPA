package it.unibo.arces.wot.sepa.engine.processing.endpoint;


import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.Engine;

@TestMethodOrder(OrderAnnotation.class)
public class GeneralTest {


	private static SPARQL11Protocol client;

	@BeforeAll
	public static void init() throws SEPASecurityException {
		Engine engine =new Engine(new String[] {});
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		client=new SPARQL11Protocol(null);

	}


	@Test
	public void TEST_01_INSERT_DATA_NO_PREFIX(){
		cleanDataSet();
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
		String sparqlQuery = 
				"SELECT ?g ?s ?p ?o WHERE  {"
						+ "	GRAPH ?g { \r\n"
						+ "		?s ?p ?o .\r\n"
						+ "	}\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==12);
	}



	@Test
	public void TEST_02_INSERT_DATA_WITH_PREFIX(){
		cleanDataSet();
		String sparqlUpdate = 
				"PREFIX dc:<http://purl.org/dc/elements/1.1/>"
						+ "PREFIX px1:<http://test1/>"
						+ "PREFIX px2:<http://test2#>"
						+"INSERT DATA  {  \r\n"
						+ "	GRAPH <http://g1> { \r\n"
						+ "		<http://s1> px1:p1 <http://o1> .\r\n"
						+ "		<http://s2> px2:p1 <http://o3> .\r\n"
						+ "		<http://s2> px1:p2 <http://o3> .\r\n"
						+ "		<http://s3> px2:p3 <http://o3> .\r\n"
						+ "		<http://s3> dc:title <http://o3> .\r\n"
						+ "	}\r\n"
						+ "}	";
		String sparqlQuery = 
				"SELECT ?g ?s ?p ?o WHERE  {"
						+ "	GRAPH ?g { \r\n"
						+ "		?s ?p ?o .\r\n"
						+ "	}\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==5);
	}

	@Test
	public void TEST_03_DATATYPE(){
		cleanDataSet();
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\"^^xsd:boolean.\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\" .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlQuery = 
				"SELECT ?g WHERE  {"
						+ "	GRAPH ?g { \r\n"
						+ "		<http://s1> <http://p2> \"true\" .\r\n"
						+ "	}\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}
	
	@Test
	public void TEST_04_FROM(){
		cleanDataSet();
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\"^^xsd:boolean.\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\" .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://a> <http://b> <http://c> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g4> { \r\n"
				+ "		<http://a> <http://b> <http://c> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlQuery = 
				"SELECT ?s FROM <http://g4> WHERE  {"
						+ "	?s <http://b> ?o .\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}

	@Test
	public void TEST_05_FROM_NAMED(){
		cleanDataSet();
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\"^^xsd:boolean.\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\" .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://a> <http://b> <http://c_3> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_3> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g4> { \r\n"
				+ "		<http://a> <http://b> <http://c_4> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_4> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g5> { \r\n"
				+ "		<http://a> <http://b> <http://c_5> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_5> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlQuery = 
				"SELECT ?s FROM NAMED <http://g3> FROM NAMED <http://g5> WHERE  {"
						+ "	GRAPH ?g { \r\n"
						+ "	?s <http://b> ?o .\r\n"
						+ "	}\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==2);
	}
	
	@Test
	public void TEST_06_FROM_NAMED_and_FROM(){
		cleanDataSet();
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"true\"^^xsd:boolean.\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://s1> <http://p1> <http://c_4> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://a> <http://b> <http://c_3> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_3> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g4> { \r\n"
				+ "		<http://a> <http://b> <http://c_4> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_4> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g5> { \r\n"
				+ "		<http://a> <http://b> <http://c_5> .\r\n"
				+ "		<http://a1> <http://b1> <http://c1_5> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlQuery = 
				"SELECT ?s FROM <http://g2>  FROM NAMED <http://g3> FROM NAMED <http://g4> WHERE  {"
						+ "?s1 ?p1 ?o .\r\n"
						+ "	GRAPH ?g { \r\n"
						+ "	?s <http://b> ?o .\r\n"
						+ "	}\r\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}
	
	@Test
	public void TEST_07_INNESTED_QUERY(){
		cleanDataSet();
		String sparqlUpdate = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"CIAO\".\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://subject> { \r\n"
				+ "		<http://A> <http://def> <http://s1> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://pred> { \r\n"
				+ "		<http://B> <http://def> <http://p2> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlQuery = 
				"SELECT ?o WHERE  {"
				+"GRAPH ?g { ?s ?p ?o}\n"
						+ "{ SELECT ?s WHERE { GRAPH  <http://subject> {?x1 <http://def> ?s.}}}\n"
						+ "{ SELECT ?p WHERE { GRAPH  <http://pred> {?x2 <http://def> ?p.}}}\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}
	
	@Test
	public void TEST_08_WITH(){
		cleanDataSet();
		String sparqlUpdate1 = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"CIAO\".\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://A> <http://A1> <http://A2> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://B> <http://B1> <http://B3> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlUpdate2 =
				"WITH <http://g2> DELETE  {  \r\n"
				+" ?s ?p ?o ."
				+ "} INSERT { <http://C> <http://C1> <http://C2> }	"
				+ " WHERE { ?s ?p ?o }	";
		String sparqlQuery = 
				"SELECT ?o WHERE  {"
				+"GRAPH ?g { <http://C> <http://C1> ?o}\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate1);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		UpdateRequest reqUpdate2= TestUtils.generateUpdate(sparqlUpdate2);
		Response responseUpdate2 = client.update(reqUpdate2);
		assertTrue(!responseUpdate2.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}
	
	@Test
	public void TEST_09_DELETE_DATA(){
		cleanDataSet();
		String sparqlUpdate1 = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"CIAO\".\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://A> <http://A1> <http://A2> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://B> <http://B1> <http://B3> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlUpdate2 =
				" DELETE  DATA {  \r\n"
				+" GRAPH <http://g1> {<http://s1> <http://p1> <http://o1> } ."
				+ "} ";
		String sparqlQuery = 
				"SELECT ?s ?p ?o WHERE  {"
				+"GRAPH <http://g1> { ?s ?p ?o}\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate1);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		UpdateRequest reqUpdate2= TestUtils.generateUpdate(sparqlUpdate2);
		Response responseUpdate2 = client.update(reqUpdate2);
		assertTrue(!responseUpdate2.isError());
		QueryRequest reqQuery= TestUtils.generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==1);
	}
	
	
	@Test
	public void TEST_10_DELETE_INSERT(){
		cleanDataSet();
		String sparqlUpdate1 = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"CIAO\".\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://A> <http://A1> <http://A2> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://B> <http://B1> <http://B3> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlUpdate2 =
				" DELETE {  \r\n"
				+" GRAPH ?g {<http://s1> <http://p1> <http://o1> } ."
				+ "} INSERT { GRAPH <http://newGRAPH> { <http://s> <http://p> <http://o>}}\n"
				+ " WHERE { GRAPH ?g {<http://s1> <http://p1> <http://o1> } } ";
		String sparqlQuery1 = 
				"SELECT ?s ?p ?o WHERE  {"
				+"GRAPH <http://g1> { ?s ?p ?o}\n"
						+ "}	";
		String sparqlQuery2 = 
				"SELECT ?s ?p ?o WHERE  {"
				+"GRAPH <http://newGRAPH> { ?s ?p ?o}\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate1);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		UpdateRequest reqUpdate2= TestUtils.generateUpdate(sparqlUpdate2);
		Response responseUpdate2 = client.update(reqUpdate2);
		assertTrue(!responseUpdate2.isError());
		
		QueryRequest reqQuery1= TestUtils.generateQuery(sparqlQuery1);
		Response responseQuery1 = client.query(reqQuery1);
		assertTrue(!responseQuery1.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery1).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery1).getBindingsResults().getBindings().size()==1);

		QueryRequest reqQuery2= TestUtils.generateQuery(sparqlQuery2);
		Response responseQuery2 = client.query(reqQuery2);
		assertTrue(!responseQuery2.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery2).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery2).getBindingsResults().getBindings().size()==1);
	}
	

	@Test
	public void TEST_11_DELETE_WHERE(){
		cleanDataSet();
		String sparqlUpdate1 = "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n"+
				"INSERT DATA  {  \r\n"
				+ "	GRAPH <http://g1> { \r\n"
				+ "		<http://s1> <http://p1> <http://o1> .\r\n"
				+ "		<http://s1> <http://p2> \"CIAO\".\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g2> { \r\n"
				+ "		<http://A> <http://A1> <http://A2> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g3> { \r\n"
				+ "		<http://B> <http://B1> <http://B3> .\r\n"
				+ "	}\r\n"
				+ "	GRAPH <http://g4> { \r\n"
				+ "		<http://s1> <http://p1> <http://o10> .\r\n"
				+ "	}\r\n"
				+ "}	";
		String sparqlUpdate2 =
				" DELETE WHERE {  \r\n"
				+" GRAPH ?g {<http://s1> ?p ?o }\n GRAPH ?g { ?s ?p ?o }\n }";
		String sparqlQuery1 = 
				"SELECT ?s ?p ?o WHERE  {"
				+"GRAPH <http://g1> { ?s ?p ?o}\n"
						+ "}	";
		String sparqlQuery2 = 
				"SELECT ?s ?p ?o WHERE  {"
				+"GRAPH <http://g4> { ?s ?p ?o}\n"
						+ "}	";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate1);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		UpdateRequest reqUpdate2= TestUtils.generateUpdate(sparqlUpdate2);
		Response responseUpdate2 = client.update(reqUpdate2);
		assertTrue(!responseUpdate2.isError());
		
		QueryRequest reqQuery1= TestUtils.generateQuery(sparqlQuery1);
		Response responseQuery1 = client.query(reqQuery1);
		assertTrue(!responseQuery1.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery1).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery1).getBindingsResults().getBindings().size()==0);

		QueryRequest reqQuery2= TestUtils.generateQuery(sparqlQuery2);
		Response responseQuery2 = client.query(reqQuery2);
		assertTrue(!responseQuery2.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery2).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery2).getBindingsResults().getBindings().size()==0);
	}
	
	private static void cleanDataSet() {
		String sparqlUpdate = "DELETE WHERE { GRAPH ?g { ?s ?p ?o.}}";
		UpdateRequest reqUpdate= TestUtils.generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
	}


}



