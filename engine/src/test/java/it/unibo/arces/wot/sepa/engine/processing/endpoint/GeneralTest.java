package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.core.Engine;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GeneralTest {
	
	private static final UpdateHTTPMethod updateMethod = UpdateHTTPMethod.POST;
	private static final QueryHTTPMethod queryMethod = QueryHTTPMethod.POST;
	private static final String scheme = "http";
	private static final String host = "localhost";
	private static final int port = 8000;
	private static final String updatePath = "/update";
	private static final String queryPath = "/query";
	
	private static SPARQL11Protocol client;
	
	@BeforeClass
	public static void init() throws SEPASecurityException {
		Engine engine =new Engine(new String[] {});
		client=new SPARQL11Protocol(null);
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlQuery);
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
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlQuery);
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
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlQuery);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		System.out.println("TEST: "+((QueryResponse)responseQuery).getBindingsResults().toString());
		assertTrue(((QueryResponse)responseQuery).getBindingsResults().getBindings().size()==2);
	}
	
	private static void cleanDataSet() {
		String sparqlUpdate = "DELETE WHERE { GRAPH ?g { ?s ?p ?o.}}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
	}
	
	private static UpdateRequest generateUpdate(String sparql) {
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
	
	private static QueryRequest generateQuery(String sparql) {
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



