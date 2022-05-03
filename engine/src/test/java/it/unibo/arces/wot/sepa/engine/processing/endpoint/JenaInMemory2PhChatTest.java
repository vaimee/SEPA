package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import static org.junit.Assert.assertFalse;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
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
	private static String room_graph="http://wot.arces.unibo.it/chat/room_default";
	private static String graphUri="http://wot.arces.unibo.it/chat/";
	private static String graph="<"+graphUri+">";
	private static String sender="chat:IamASender";
	private static String receiver="chat:IamAReceiver";
	private static String senderName="Sender";
	private static String receiverName="Receiver";
	private static String messageid="<http://messageid>";
	private static String partecipant_graph="<http://wot.arces.unibo.it/chat/partecipants>";
	
	
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


//
	@Test
	public void TEST_02_SEND() throws SEPASecurityException {
		String sparqlUpdate = prefixs+
						"INSERT {\r\n"
					+ "						GRAPH <"+room_graph+"> {"
					+ 							messageid+" rdf:type schema:Message ;\r\n"
					+ "							schema:text \"Testo del messaggio\" ;\r\n"
					+ "							schema:sender "+sender+" ;\r\n"
					+ "							schema:toRecipient "+receiver+";\r\n"
					+ "							schema:dateSent ?time.\r\n"
					+			 			"}"
					+ "					} WHERE {\r\n"
					+ "						BIND(\""+new Date().toString()+"\" AS ?time) ."
					+ "						GRAPH "+graph+" {\r\n"
					+ "							"+sender+" rdf:type schema:Person .\r\n"
					+ "							"+receiver+" rdf:type schema:Person \r\n"
					+ "						}\r\n"
					+ "					}	";
		Response res= inMemEndPoint.update(sparqlUpdate);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateRes = (UpdateResponse)res;
//			LUTTTestUtils.printQueryAll(inMemEndPoint);
			Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message"));
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/text","Testo del messaggio"));
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/sender","http://wot.arces.unibo.it/chat#IamASender"));
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/toRecipient","http://wot.arces.unibo.it/chat#IamAReceiver"));
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/dateSent",null));
			assertTrue(TestUtils.quadsSetCompare(updateRes.updatedTuples,expectedAdded,"02.added"));
			assertTrue(updateRes.removedTuples.size()==0);
			
		}

	}
	
	@Test
	public void TEST_02V2_SEND() throws SEPASecurityException {
		String sparqlUpdate = "PREFIX schema:<http://schema.org/> PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX owl:<http://www.w3.org/2002/07/owl#> PREFIX chat:<http://wot.arces.unibo.it/chat#> PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> \r\n"
				+ "		INSERT {\r\n"
				+ "			GRAPH <http://wot.arces.unibo.it/chat/room_2_4> {\r\n"
				+ "				?message rdf:type schema:Message ;\r\n"
				+ "				schema:text \"MSG #4\" ;\r\n"
				+ "				schema:sender <http://wot.arces.unibo.it/chat/person_9f9e9035-6e28-456d-bb0a-96bc8f8fc91f> ;\r\n"
				+ "				schema:toRecipient <http://wot.arces.unibo.it/chat/person_28a2becf-6e62-4227-9d65-cfc55c8da99f>;\r\n"
				+ "				schema:dateSent ?time\r\n"
				+ "			}\r\n"
				+ "		} WHERE    {\r\n"
				+ "			BIND(IRI(CONCAT(CONCAT(str(<http://wot.arces.unibo.it/chat/room_2_4>) ,\"/message_\"),STRUUID())) AS ?message) .\r\n"
				+ "			BIND(STR('2022-04-22T15:26:15.568327Z'^^xsd:dateTimeStamp) AS ?time) .\r\n"
				+ "		}";
		Response res= inMemEndPoint.update(sparqlUpdate);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateRes = (UpdateResponse)res;
//			LUTTTestUtils.printQueryAll(inMemEndPoint);
			Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedAdded.add(new TempQuadForTest("http://wot.arces.unibo.it/chat/room_2_4",null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message"));
			expectedAdded.add(new TempQuadForTest("http://wot.arces.unibo.it/chat/room_2_4",null,"http://schema.org/text","MSG #4"));
			expectedAdded.add(new TempQuadForTest("http://wot.arces.unibo.it/chat/room_2_4",null,"http://schema.org/sender","http://wot.arces.unibo.it/chat/person_9f9e9035-6e28-456d-bb0a-96bc8f8fc91f"));
			expectedAdded.add(new TempQuadForTest("http://wot.arces.unibo.it/chat/room_2_4",null,"http://schema.org/toRecipient","http://wot.arces.unibo.it/chat/person_28a2becf-6e62-4227-9d65-cfc55c8da99f"));
			expectedAdded.add(new TempQuadForTest("http://wot.arces.unibo.it/chat/room_2_4",null,"http://schema.org/dateSent",null));
			assertTrue(TestUtils.quadsSetCompare(updateRes.updatedTuples,expectedAdded,"02.added"));
			assertTrue(updateRes.removedTuples.size()==0);
			
		}

	}
	
	
	
	@Test
	public void TEST_03_SENT() throws SEPASecurityException {
		String sparqlQuery = prefixs+
						"SELECT ?message ?sender ?name ?text ?time \r\n"
						+ "				FROM <"+room_graph+"> \r\n"
						+ "				FROM NAMED "+graph+" \r\n"
						+ "				WHERE {\r\n"
						+ "					?message rdf:type schema:Message ;\r\n"
						+ "					schema:text ?text ;\r\n"
						+ "					schema:sender ?sender ;					\r\n"
						+ "					schema:toRecipient "+receiver+";\r\n"
						+ "					schema:dateSent ?time .\r\n"
						+ "\r\n"
						+ "					GRAPH "+graph+" {\r\n"
						+ "						?sender rdf:type schema:Person ;\r\n"
						+ "						schema:name ?name .\r\n"
						+ "						"+receiver+" rdf:type schema:Person\r\n"
						+ "					}\r\n"
						+ "\r\n"
						+ "\r\n"
						+ "} ORDER BY ?time";
		Response res= inMemEndPoint.query(sparqlQuery);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			QueryResponse queryeRes = (QueryResponse)res;
			assertTrue(queryeRes.getBindingsResults().getBindings().size()==1);
		}

	}
	
	@Test
	public void TEST_04_SET_RECEIVED() throws SEPASecurityException {
		String sparqlUpdate = prefixs+
					"WITH <"+room_graph+"> DELETE {\r\n"
					+ "							"+messageid+" schema:dateReceived ?time .\r\n"
					+ "							"+messageid+" chat:atualReceivedCount ?count .\r\n"
					+ "					}\r\n"
					+ "					INSERT {\r\n"
					+ "							"+messageid+" schema:dateReceived ?time .\r\n"
					+ "							"+messageid+" chat:atualReceivedCount ?countupdated.\r\n"
					+ "					} WHERE {	\r\n"
					+ "						OPTIONAL{"+messageid+" chat:atualReceivedCount ?count.}\r\n"
					+ "						"+messageid+" rdf:type schema:Message .	\r\n"
					+ "						BIND(STR(now()) AS ?time) .\r\n"
					+ "						BIND ((IF(BOUND(?count), ?count + 1, 1)) AS ?countupdated) .\r\n"
					+ "					}";
		Response res= inMemEndPoint.update(sparqlUpdate);
//		LUTTTestUtils.printQueryAll(inMemEndPoint);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateRes = (UpdateResponse)res;			
			Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/dateReceived",null));
			expectedAdded.add(new TempQuadForTest(room_graph,"http://messageid","http://wot.arces.unibo.it/chat#atualReceivedCount","1"));
			assertTrue(TestUtils.quadsSetCompare(updateRes.updatedTuples,expectedAdded,"03.added"));
			assertTrue(updateRes.removedTuples.size()==0);		
		}
	}
	

	@Test
	public void TEST_05_REMOVE() throws SEPASecurityException {
		String sparqlUpdate = prefixs+
					"WITH <"+room_graph+"> DELETE \r\n"
					+ "				{"+messageid+" ?p ?o} \r\n"
					+ "			WHERE {\r\n"
					+ "					"+messageid+" rdf:type schema:Message ; ?p ?o.					\r\n"
					+ "					OPTIONAL{"+partecipant_graph+" rdf:value ?participants .}			\r\n"
					+ "				 	OPTIONAL{"+messageid+" chat:atualReceivedCount ?count.}\r\n"
					+ "					BIND ((IF(BOUND(?count), ?count, 1)) AS ?countBinded).						\r\n"
					+ "					BIND ((IF(BOUND(?participants), ?participants, 1)) AS ?participantsBinded).			\r\n"
					+ "					FILTER (?countBinded >= ?participantsBinded).\r\n"
					+ "			}";
		Response res= inMemEndPoint.update(sparqlUpdate);
//		LUTTTestUtils.printQueryAll(inMemEndPoint);
		if(res.isError()) {
			System.out.println(((ErrorResponse)res).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateRes = (UpdateResponse)res;			
			Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/dateReceived",null));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://wot.arces.unibo.it/chat#atualReceivedCount","1"));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message"));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/text","Testo del messaggio"));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/sender","http://wot.arces.unibo.it/chat#IamASender"));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/toRecipient","http://wot.arces.unibo.it/chat#IamAReceiver"));
			expectedRemoved.add(new TempQuadForTest(room_graph,"http://messageid","http://schema.org/dateSent",null));
			
			assertTrue(TestUtils.quadsSetCompare(updateRes.removedTuples,expectedRemoved,"04.removed"));
			assertTrue(updateRes.updatedTuples.size()==0);		
		}
	}
	
//	@Test
//	public void TEST_0X_CREATE_ROOM() throws SEPASecurityException {
//		String sparqlUpdate = prefixs+
//					"WITH  <"+room_graph+">\r\n"
//					+ "					INSERT DATA {\r\n"
//					+ "						"+partecipant_graph+"  rdf:type chat:actualSize;\r\n"
//					+ "						 rdf:value 0.\r\n"
//					+ "					}";
//		printQueryAll();
//		Response res= inMemEndPoint.update(sparqlUpdate);
//		if(res.isError()) {
//			System.out.println(((ErrorResponse)res).getErrorDescription());
//			assertTrue(false);
//		}else {
//			UpdateResponse updateRes = (UpdateResponse)res;
//			printQueryAll();
//		}
//	}
//	
//	@Test
//	public void TEST_0X_ENTER_ROOM() throws SEPASecurityException {
//		String sparqlUpdate = prefixs+
//					"			WITH  "+room_graph+"\r\n"
//					+ "				DELETE {\r\n"
//					+ "					"+partecipant_graph+" rdf:value ?oldValue \r\n"
//					+ "				}\r\n"
//					+ "				INSERT {\r\n"
//					+ "					"+partecipant_graph+"  rdf:value ?newValue \r\n"
//					+ "				}\r\n"
//					+ "				WHERE {	\r\n"
//					+ "					OPTIONAL{"+partecipant_graph+"  rdf:value  ?oldValue } \r\n"
//					+ "					BIND ((IF(BOUND(?oldValue), ?oldValue + 1, 1)) AS ?newValue )\r\n"
//					+ "				}";
//		printQueryAll();
//		Response res= inMemEndPoint.update(sparqlUpdate);
//		if(res.isError()) {
//			System.out.println(((ErrorResponse)res).getErrorDescription());
//			assertTrue(false);
//		}else {
//			UpdateResponse updateRes = (UpdateResponse)res;
//			printQueryAll();
//		}
//	}
//	
//	@Test
//	public void TEST_0_STORE_SENT() throws SEPASecurityException {
//	
//	}
//	@Test
//	public void TEST_0_STORE_RECEIVED() throws SEPASecurityException {
//	
//	}
	@Test
	public void TEST_01_REGISTER_USER() throws SEPASecurityException {
		String sparqlUpdateR = prefixs
				+ "DELETE { GRAPH "+graph+" {?x rdf:type schema:Person . ?x schema:name \""+receiverName+"\"}}"
				+ "INSERT { GRAPH "+graph+" { ?person rdf:type schema:Person ; schema:name \""+receiverName+"\"}} "
				+ "WHERE {BIND("+receiver+" AS ?person) "
				+ "OPTIONAL {?x rdf:type schema:Person . ?x schema:name \""+receiverName+"\"}}";
		String sparqlUpdateS = prefixs
				+ "DELETE { GRAPH "+graph+" {?x rdf:type schema:Person . ?x schema:name \""+senderName+"\"}}"
				+ "INSERT { GRAPH "+graph+" { ?person rdf:type schema:Person ; schema:name \""+senderName+"\"}} "
				+ "WHERE {BIND("+sender+" AS ?person) "
				+ "OPTIONAL {?x rdf:type schema:Person . ?x schema:name \""+senderName+"\"}}";
//		printQueryAll();
		Response resR= inMemEndPoint.update(sparqlUpdateR);
		Response resS= inMemEndPoint.update(sparqlUpdateS);
		if(resR.isError()) {
			System.out.println(((ErrorResponse)resR).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateResR = (UpdateResponse)resR;
			Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedAdded.add(new TempQuadForTest(graphUri,"http://wot.arces.unibo.it/chat#IamAReceiver","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person"));
			expectedAdded.add(new TempQuadForTest(graphUri,"http://wot.arces.unibo.it/chat#IamAReceiver","http://schema.org/name",receiverName));
			assertTrue(TestUtils.quadsSetCompare(updateResR.updatedTuples,expectedAdded,"01.r.added"));
			assertTrue(updateResR.removedTuples.size()==0);
		}
		if(resS.isError()) {
			System.out.println(((ErrorResponse)resS).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponse updateResS = (UpdateResponse)resS;
			Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
			//order TempQuadForTest args: graph, subject, predicate, object
			expectedAdded.add(new TempQuadForTest(graphUri,"http://wot.arces.unibo.it/chat#IamASender","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person"));
			expectedAdded.add(new TempQuadForTest(graphUri,"http://wot.arces.unibo.it/chat#IamASender","http://schema.org/name",senderName));
			assertTrue(TestUtils.quadsSetCompare(updateResS.updatedTuples,expectedAdded,"01.s.added"));
			assertTrue(updateResS.removedTuples.size()==0);
		}
	}
	
//	@Test
//	public void TEST_0_DELETE_ROOM() throws SEPASecurityException {
//	
//	}


	
}



