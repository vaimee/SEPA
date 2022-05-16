package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static org.junit.Assert.assertTrue;


import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.core.Engine;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ChatTest {

	/*
	 * NOTE: the count of quads can changed if you do not use JENA AR as endpoint
	 */
	private static final UpdateHTTPMethod updateMethod = UpdateHTTPMethod.POST;
	private static final QueryHTTPMethod queryMethod = QueryHTTPMethod.POST;
	private static final String scheme = "http";
	private static final String host = "localhost";
	private static final int port = 8000;
	private static final String updatePath = "/update";
	private static final String queryPath = "/query";

	private static final String sparqlCountQuad = 
			"SELECT (COUNT(?quad) AS ?count) WHERE  {"
					+ "	GRAPH ?g { \r\n"
					+ "		?s ?p ?o .\r\n"
					+ "	}\r\n"
					+ "BIND(CONCAT(STR(?g),CONCAT(STR(?s),CONCAT(STR(?p),STR(?o)))) AS ?quad)"
					+ "}";

	private static final String prefixs = 
			"PREFIX chat:<http://wot.arces.unibo.it/chat/>\r\n"
					+ "PREFIX schema:<http://schema.org/>\r\n"
					+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
					+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\r\n";

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
	public void TEST_01_DELETE_ALL() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 0;
		String sparqlUpdate = 
				"DELETE WHERE { GRAPH ?g { ?s ?p ?o } }";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_02_REGISTER_USER() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 10;

		QueryRequest reqQuery2= generateQuery(sparqlCountQuad);
		Response responseQuery2 = client.query(reqQuery2);
		assertTrue(!responseQuery2.isError());
		int temp = Integer.parseInt(((QueryResponse)responseQuery2).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue());
		String sparqlUpdate = prefixs
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "        ?person chat:status chat:registered;\r\n"
				+ "            chat:username \"TSG\" . \r\n"
				+ "    } \r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        ?person rdf:type schema:Person ;\r\n"
				+ "                chat:name \"Andrea\" ;\r\n"
				+ "                chat:surname \"Ferrari\" ;\r\n"
				+ "                chat:gender \"M\" .\r\n"
				+ "        chat:count chat:send 0 .\r\n"
				+ "        chat:count chat:received 0 .\r\n"
				+ "        chat:count chat:confirmed 0 .\r\n"
				+ "        chat:count chat:deleted 0 .\r\n"
				+ "    }    \r\n"
				+ "} WHERE {\r\n"
				+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\"TSG\")) AS ?person).\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_03_REGISTER_USER() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 20;
		String sparqlUpdate = prefixs
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "        ?person chat:status chat:registered;\r\n"
				+ "            chat:username \"TSG2\" . \r\n"
				+ "    } \r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        ?person rdf:type schema:Person ;\r\n"
				+ "                chat:name \"PIPPO\" ;\r\n"
				+ "                chat:surname \"surname\" ;\r\n"
				+ "                chat:gender \"M\" .\r\n"
				+ "        chat:count chat:send 0 .\r\n"
				+ "        chat:count chat:received 0 .\r\n"
				+ "        chat:count chat:confirmed 0 .\r\n"
				+ "        chat:count chat:deleted 0 .\r\n"
				+ "    }    \r\n"
				+ "} WHERE {\r\n"
				+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\"TSG2\")) AS ?person).\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_04_CREATE_ROOM() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 22;
		String sparqlUpdate = prefixs					
				+ "INSERT DATA {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/partecipants> chat:count 0 .\r\n"
				+ "        <http://wot.arces.unibo.it/chat/room> rdf:type chat:type_msg .\r\n"
				+ "    }\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_05_ENTER_ROOM() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 23;
		String sparqlUpdate = prefixs
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
				+ "DELETE {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants chat:count ?oldValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants chat:count ?newValue .\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG> .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "WHERE {	\r\n"
				+ "    OPTIONAL{GRAPH <http://wot.arces.unibo.it/chat/room/a> { chat:partecipants  chat:count  ?oldValue }} \r\n"
				+ "    BIND ((IF(BOUND(?oldValue), ?oldValue + 1, 1)) AS ?newValue )\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		System.out.println(((QueryResponse)responseQuery).getBindingsResults());
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}


	@Test
	public void TEST_06_ENTER_ROOM() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 24;
		String sparqlUpdate = prefixs
				+ "DELETE {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants chat:count ?oldValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants chat:count ?newValue .\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG2> .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "WHERE {	\r\n"
				+ "    OPTIONAL{GRAPH <http://wot.arces.unibo.it/chat/room/a> { chat:partecipants  chat:count  ?oldValue }} \r\n"
				+ "    BIND ((IF(BOUND(?oldValue), ?oldValue + 1, 1)) AS ?newValue )\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_07_SEND() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 29;
		String sparqlUpdate = prefixs
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        ?message rdf:type chat:type_msg ;\r\n"
				+ "        chat:content \"msg 1\" ;\r\n"
				+ "        schema:sender <http://wot.arces.unibo.it/chat/person_TSG> ;\r\n"
				+ "        schema:toRecipient <http://wot.arces.unibo.it/chat/person_TSG2>;\r\n"
				+ "        schema:dateSent ?time .\r\n"
				+ "    }\r\n"
				+ "} WHERE    {\r\n"
				+ "    BIND(IRI(CONCAT(CONCAT(str(<http://wot.arces.unibo.it/chat/room/a>) ,\"/message_\"),STRUUID())) AS ?message) .\r\n"
				+ "    BIND(STR(now()) AS ?time) .\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG> chat:status chat:registered .\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG2> chat:status chat:registered .\r\n"
				+ "    }\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG> .\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG2> .\r\n"
				+ "    }\r\n"
				+ "}	";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_08_LOG_SEND() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 29;
		String sparqlUpdate = prefixs
				+ "DELETE {\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:count chat:send ?oldValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "INSERT {\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:partecipants chat:count ?newValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "WHERE {	\r\n"
				+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\"TSG\")) AS ?person).\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:count chat:send ?oldValue .\r\n"
				+ "    }\r\n"
				+ "    BIND ((?oldValue + 1) AS ?newValue )\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_09_QUERY_RECEIVED() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "	SELECT ?message ?sender ?type ?content ?time \r\n"
				+ "				WHERE {\r\n"
				+ "					GRAPH ?room {\r\n"
				+ "						?message rdf:type ?type ;\r\n"
				+ "						chat:content ?content ;\r\n"
				+ "						schema:sender ?sender ;					\r\n"
				+ "						schema:toRecipient ?receiver;\r\n"
				+ "						schema:dateSent ?time .\r\n"
				+ "						chat:partecipants rdf:value ?receiver.\r\n"
				+ "						chat:partecipants rdf:value ?sender.\r\n"
				+ "					}\r\n"
				+ "					GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "						?receiver chat:status chat:registered .\r\n"
				+ "						?sender chat:status chat:registered .\r\n"
				+ "					}\r\n"
				+ "				} ORDER BY ?time";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}

	@Test
	public void TEST_10_SEND() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 34;
		String sparqlUpdate = prefixs
				+ "INSERT {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        ?message rdf:type chat:type_msg ;\r\n"
				+ "        chat:content \"msg 2\" ;\r\n"
				+ "        schema:sender <http://wot.arces.unibo.it/chat/person_TSG2>;\r\n"
				+ "        schema:toRecipient <http://wot.arces.unibo.it/chat/person_TSG>;\r\n"
				+ "        schema:dateSent ?time .\r\n"
				+ "    }\r\n"
				+ "} WHERE    {\r\n"
				+ "    BIND(IRI(CONCAT(CONCAT(str(<http://wot.arces.unibo.it/chat/room/a>) ,\"/message_\"),STRUUID())) AS ?message) .\r\n"
				+ "    BIND(STR(now()) AS ?time) .\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG> chat:status chat:registered .\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG2> chat:status chat:registered .\r\n"
				+ "    }\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG> .\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG2> .\r\n"
				+ "    }\r\n"
				+ "}	";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_11_LOG_SEND() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 34;
		String sparqlUpdate = prefixs
				+ "DELETE {\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:count chat:send ?oldValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "INSERT {\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:partecipants chat:count ?newValue .\r\n"
				+ "    }\r\n"
				+ "}\r\n"
				+ "WHERE {	\r\n"
				+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\"TSG2\")) AS ?person).\r\n"
				+ "    GRAPH ?person {\r\n"
				+ "        chat:count chat:send ?oldValue .\r\n"
				+ "    }\r\n"
				+ "    BIND ((?oldValue + 1) AS ?newValue )\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_12_SET_RECEIVED() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 35;
		String sparqlUpdate = prefixs
				+ "INSERT DATA {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/msg_id> chat:received \"true\"^^xsd:boolean.\r\n"
				+ "    }\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}
	
	@Test
	public void TEST_12_SET_RECEIVED_bis() throws NumberFormatException, SEPABindingsException{
		//same of test 12 but we use ' instead of "
		int datasetSizeAfterUpdate = 36;
		String sparqlUpdate = prefixs
				+ "INSERT DATA {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/msg_id2> chat:received 'true'^^xsd:boolean.\r\n"
				+ "    }\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_13_DELETE_MSG() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 31;
		String sparqlUpdate = prefixs
				+ "DELETE {\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        ?message ?p ?o.\r\n"
				+ "    }\r\n"
				+ "} WHERE    {\r\n"
				+ "   	GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"	
				+ "       ?message ?p ?o;\r\n"				
				+ "        schema:sender <http://wot.arces.unibo.it/chat/person_TSG>;\r\n"
				+ "        schema:toRecipient <http://wot.arces.unibo.it/chat/person_TSG2>.\r\n"
				+ "    }\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG> chat:status chat:registered .\r\n"
				+ "        <http://wot.arces.unibo.it/chat/person_TSG2> chat:status chat:registered .\r\n"
				+ "    }\r\n"
				+ "    GRAPH <http://wot.arces.unibo.it/chat/room/a> {\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG> .\r\n"
				+ "        chat:partecipants rdf:value <http://wot.arces.unibo.it/chat/person_TSG2> .\r\n"
				+ "    }\r\n"
				+ "}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}

	@Test
	public void TEST_14_CPU_STATUS() throws NumberFormatException, SEPABindingsException{
		int datasetSizeAfterUpdate = 35;
		String sparqlUpdate = prefixs
				+ "DELETE {\r\n"
				+ "	    GRAPH <http://wot.arces.unibo.it/chat/hw/cpu> {\r\n"
				+ "	        ?s ?p ?o.\r\n"
				+ "	    }\r\n"
				+ "	} INSERT {\r\n"
				+ "	      GRAPH <http://wot.arces.unibo.it/chat/hw/cpu> {\r\n"
				+ "	        <http://wot.arces.unibo.it/chat/hw/cpu/core/01> rdf:value 80 .\r\n"
				+ "	        <http://wot.arces.unibo.it/chat/hw/cpu/core/02> rdf:value 70 .\r\n"
				+ "	        <http://wot.arces.unibo.it/chat/hw/cpu/core/03> rdf:value 50 .\r\n"
				+ "	        <http://wot.arces.unibo.it/chat/hw/cpu/core/04> rdf:value 10 .\r\n"
				+ "	    }\r\n"
				+ "	} WHERE    {\r\n"
				+ "	   	OPTIONAL{\r\n"
				+ "	        GRAPH <http://wot.arces.unibo.it/chat/hw/cpu> {\r\n"
				+ "	            ?s ?p ?o.\r\n"
				+ "	        }\r\n"
				+ "	    }\r\n"
				+ "	}";
		UpdateRequest reqUpdate= generateUpdate(sparqlUpdate);
		Response responseUpdate = client.update(reqUpdate);
		assertTrue(!responseUpdate.isError());
		QueryRequest reqQuery= generateQuery(sparqlCountQuad);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
		assertTrue(Integer.parseInt(((QueryResponse)responseQuery).getBindingsResults().getBindings().get(0).getRDFTerm("count").getValue())==datasetSizeAfterUpdate);
	}


	@Test
	public void TEST_15_QUERY_ROOM() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "	SELECT ?p ?o WHERE { GRAPH ?g {chat:partecipants ?p ?o.}}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}
	
	@Test
	public void TEST_16_QUERY_CPU() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "SELECT ?s ?p ?o WHERE { GRAPH <http://wot.arces.unibo.it/chat/hw/cpu> {?s ?p ?o.}}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}
	
	@Test
	public void TEST_17_QUERY_LOG_MONITOR() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "SELECT ?person ?log ?count WHERE { \r\n"
				+ "		GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "			?person chat:status chat:registered.\r\n"
				+ "		} \r\n"
				+ "		GRAPH ?person {\r\n"
				+ "			?person rdf:type schema:Person.\r\n"
				+ "			?log chat:count ?count.\r\n"
				+ "		}\r\n"
				+ "	}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}
	
	@Test
	public void TEST_18_QUERY_LEAKER_1() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "SELECT ?content WHERE { \r\n"
				+ "				 GRAPH ?g {\r\n"
				+ "					?message chat:content ?content.\r\n"
				+ "					chat:room rdf:type chat:type_url.\r\n"
				+ "				}\r\n"
				+ "			}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}
	
	@Test
	public void TEST_19_QUERY_LEAKER_2() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "SELECT ?person WHERE { \r\n"
				+ "				GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "					?person chat:status chat:registered .\r\n"
				+ "				}\r\n"
				+ "			}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
	}
	
	@Test
	public void TEST_20_QUERY_LEAKER_3() throws NumberFormatException, SEPABindingsException{
		String sparqlQ = prefixs
				+ "SELECT ?person ?room ?time WHERE { \r\n"
				+ "				GRAPH ?person {\r\n"
				+ "					?person rdf:type schema:Person ;\r\n"
				+ "					chat:surname \"Ferrari\" ;\r\n"
				+ "				}\r\n"
				+ "				GRAPh ?room {\r\n"
				+ "					?message schema:dateSent ?time;\r\n"
				+ "        			schema:sender ?person ;\r\n"
				+ "				}}";
		QueryRequest reqQuery= generateQuery(sparqlQ);
		Response responseQuery = client.query(reqQuery);
		assertTrue(!responseQuery.isError());
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



