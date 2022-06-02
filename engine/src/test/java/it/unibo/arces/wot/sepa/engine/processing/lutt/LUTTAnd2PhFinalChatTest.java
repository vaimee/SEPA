package it.unibo.arces.wot.sepa.engine.processing.lutt;


import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.processing.ARQuadsAlgorithm;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.SjenarEndpointDoubleStore;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.TempQuadForTest;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.TestUtils;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.ar.UpdateResponseWithAR;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LUTTAnd2PhFinalChatTest {
	
	private static SjenarEndpointDoubleStore store_ph_1;
	private static SjenarEndpointDoubleStore store_ph_2;
	
	private static String prefixs = "PREFIX schema:<http://schema.org/>\n" +
			 "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			 "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\n" +
			 "PREFIX chat:<http://wot.arces.unibo.it/chat/>\n";
	
	private static String graphUri="http://wot.arces.unibo.it/chat/";
	
	private static String sender="http://wot.arces.unibo.it/chat/person_CB1";
	private static String senderUsername="CB1";
	private static String receiver="http://wot.arces.unibo.it/chat/person_CB2";
	private static String receiverUsername="CB2";
	

	private static String room="http://wot.arces.unibo.it/chat/room_X";
	
	private static String msg="http://wot.arces.unibo.it/chat/room_X/msgID";
	
	//#################################################SUBSCRIPTIONS
		
	private static String sub_receive_B = prefixs 
			+"SELECT ?message ?sender ?type ?content ?time \r\n"
			+ "				WHERE {\r\n"
			+ "					GRAPH <"+room+"> {\r\n"
			+ "						?message rdf:type ?type ;\r\n"
			+ "						chat:content ?content ;\r\n"
			+ "						schema:sender ?sender ;					\r\n"
			+ "						schema:toRecipient <"+receiver+">;\r\n"
			+ "						schema:dateSent ?time .\r\n"
			+ "						chat:partecipants rdf:value <"+receiver+">.\r\n"
			+ "						chat:partecipants rdf:value ?sender.\r\n"
			+ "					}\r\n"
			+ "					GRAPH <"+graphUri+"> {\r\n"
			+ "						<"+receiver+"> chat:status chat:registered .\r\n"
			+ "						?sender chat:status chat:registered .\r\n"
			+ "					}\r\n"
			+ "				} ORDER BY ?time";
	
	private static String sub_received_A = prefixs 
			+"SELECT ?message ?receiver ?type ?content ?time \r\n"
			+ "				WHERE {\r\n"
			+ "					GRAPH <"+room+"> {\r\n"
			+ "						?message rdf:type ?type ;\r\n"
			+ "						chat:content ?content ;\r\n"
			+ "						schema:sender ?sender ;					\r\n"
			+ "						schema:toRecipient ?receiver ;\r\n"
			+ "						chat:received 'true'^^xsd:boolean ;\r\n"
			+ "						schema:dateSent ?time .\r\n"
			+ "						chat:partecipants rdf:value ?receiver .\r\n"
			+ "						chat:partecipants rdf:value <"+sender+"> .\r\n"
			+ "					}\r\n"
			+ "					GRAPH <"+graphUri+"> {\r\n"
			+ "						?receiver chat:status chat:registered .\r\n"
			+ "						<"+sender+"> chat:status chat:registered .\r\n"
			+ "					}\r\n"
			+ "				} ORDER BY ?time";
	
	private static String sub_room = prefixs 
			+"SELECT ?p ?o WHERE { GRAPH ?g {chat:partecipants ?p ?o.}}";
	
	private static String sub_cpu = prefixs 
			+"SELECT ?s ?p ?o WHERE { GRAPH <http://wot.arces.unibo.it/chat/hw/cpu> {?s ?p ?o.}}";
	
	private static String sub_log_send = prefixs 
			+"SELECT ?person ?log ?count WHERE { \r\n"
			+ "				GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
			+ "					?person chat:status chat:registered.\r\n"
			+ "				} \r\n"
			+ "				GRAPH ?person {\r\n"
			+ "					?person rdf:type schema:Person.\r\n"
			+ "					chat:send chat:count ?count.\r\n"
			+ "				}\r\n"
			+ "			}";
	
	private static String sub_log_received = prefixs 
			+"SELECT ?person ?log ?count WHERE { \r\n"
			+ "				GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
			+ "					?person chat:status chat:registered.\r\n"
			+ "				} \r\n"
			+ "				GRAPH ?person {\r\n"
			+ "					?person rdf:type schema:Person.\r\n"
			+ "					chat:received chat:count ?count.\r\n"
			+ "				}\r\n"
			+ "			}";
	
	private static String sub_log_deleted = prefixs 
			+"SELECT ?person ?log ?count WHERE { \r\n"
			+ "				GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
			+ "					?person chat:status chat:registered.\r\n"
			+ "				} \r\n"
			+ "				GRAPH ?person {\r\n"
			+ "					?person rdf:type schema:Person.\r\n"
			+ "					chat:deleted chat:count ?count.\r\n"
			+ "				}\r\n"
			+ "			}";
	

	
	@BeforeClass
	public static void init() throws SEPASecurityException {
		
		store_ph_1=new SjenarEndpointDoubleStore(false);
		store_ph_2=new SjenarEndpointDoubleStore(true);
		//clean dataset (this is not necessary)
		String delete_all = "DELETE WHERE { GRAPH ?g {?s ?p ?o}}";
		store_ph_1.update(TestUtils.generateUpdate(delete_all));
		store_ph_2.update(TestUtils.generateUpdate(delete_all));

	}


//
	@Test
	public void TEST_01_Register_A() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"INSERT {\r\n"
						+ "    GRAPH <"+graphUri+"> {\r\n"
						+ "        ?person chat:status chat:registered;\r\n"
						+ "            chat:username \""+senderUsername+"\" . \r\n"
						+ "    } \r\n"
						+ "    GRAPH ?person {\r\n"
						+ "        ?person rdf:type schema:Person ;\r\n"
						+ "                chat:name \"Andrea\" ;\r\n"
						+ "                chat:surname \"Ferrari\" ;\r\n"
						+ "                chat:gender \"M\" .\r\n"
						+ "        chat:send chat:count 0 .\r\n"
						+ "        chat:received chat:count 0 .\r\n"
						+ "        chat:deleted chat:count 0 .\r\n"
						+ "    }    \r\n"
						+ "} WHERE {\r\n"
						+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\""+senderUsername+"\")) AS ?person).\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(sender,sender,"http://wot.arces.unibo.it/chat/gender","M"));
		expectedAdded.add(new TempQuadForTest(sender,sender,"http://wot.arces.unibo.it/chat/name","Andrea"));
		expectedAdded.add(new TempQuadForTest(sender,sender,"http://wot.arces.unibo.it/chat/surname","Ferrari"));
		expectedAdded.add(new TempQuadForTest(graphUri,sender,"http://wot.arces.unibo.it/chat/username",senderUsername));
		expectedAdded.add(new TempQuadForTest(graphUri,sender,"http://wot.arces.unibo.it/chat/status","http://wot.arces.unibo.it/chat/registered"));
		expectedAdded.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/deleted","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/received","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/send","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(sender,sender,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person"));
		
		check(sparqlUpdate,expectedAdded,new HashSet<TempQuadForTest>());

	}
	
	@Test
	public void TEST_02_Register_B() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"INSERT {\r\n"
						+ "    GRAPH <"+graphUri+"> {\r\n"
						+ "        ?person chat:status chat:registered;\r\n"
						+ "            chat:username \""+receiverUsername+"\" . \r\n"
						+ "    } \r\n"
						+ "    GRAPH ?person {\r\n"
						+ "        ?person rdf:type schema:Person ;\r\n"
						+ "                chat:name \"Name\" ;\r\n"
						+ "                chat:surname \"Surr\" ;\r\n"
						+ "                chat:gender \"X\" .\r\n"
						+ "        chat:send chat:count 0 .\r\n"
						+ "        chat:received chat:count 0 .\r\n"
						+ "        chat:deleted chat:count 0 .\r\n"
						+ "    }    \r\n"
						+ "} WHERE {\r\n"
						+ "    BIND(IRI(CONCAT('http://wot.arces.unibo.it/chat/person_',\""+receiverUsername+"\")) AS ?person).\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(receiver,receiver,"http://wot.arces.unibo.it/chat/gender","X"));
		expectedAdded.add(new TempQuadForTest(receiver,receiver,"http://wot.arces.unibo.it/chat/name","Name"));
		expectedAdded.add(new TempQuadForTest(receiver,receiver,"http://wot.arces.unibo.it/chat/surname","Surr"));
		expectedAdded.add(new TempQuadForTest(graphUri,receiver,"http://wot.arces.unibo.it/chat/username",receiverUsername));
		expectedAdded.add(new TempQuadForTest(graphUri,receiver,"http://wot.arces.unibo.it/chat/status","http://wot.arces.unibo.it/chat/registered"));
		expectedAdded.add(new TempQuadForTest(receiver,"http://wot.arces.unibo.it/chat/deleted","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(receiver,"http://wot.arces.unibo.it/chat/received","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(receiver,"http://wot.arces.unibo.it/chat/send","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(receiver,receiver,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person"));
		
		check(sparqlUpdate,expectedAdded,new HashSet<TempQuadForTest>());

	}
	
	
	@Test
	public void TEST_03_CreateRoom() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"INSERT DATA {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        <http://wot.arces.unibo.it/chat/partecipants> chat:count 0.\r\n"
						+ "		<http://wot.arces.unibo.it/chat/room> rdf:type chat:type_text.\r\n"
						+ "    }\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/room","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://wot.arces.unibo.it/chat/type_text"));
		
		check(sparqlUpdate,expectedAdded,new HashSet<TempQuadForTest>());

	}
	
	@Test
	public void TEST_04_EnterRoom_A() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"DELETE {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        chat:partecipants chat:count ?oldValue .\r\n"
						+ "    }\r\n"
						+ "}\r\n"
						+ "INSERT {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        chat:partecipants chat:count ?newValue .\r\n"
						+ "        chat:partecipants rdf:value <"+sender+"> .\r\n"
						+ "    }\r\n"
						+ "}\r\n"
						+ "WHERE {	\r\n"
						+ "    OPTIONAL{GRAPH <"+room+"> { chat:partecipants  chat:count  ?oldValue }} \r\n"
						+ "    BIND ((IF(BOUND(?oldValue), ?oldValue + 1, 1)) AS ?newValue )\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://www.w3.org/1999/02/22-rdf-syntax-ns#value",sender));
		
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://wot.arces.unibo.it/chat/count",null));
		
		check(sparqlUpdate,expectedAdded,expectedRemoved);

	}
	
	@Test
	public void TEST_05_EnterRoom_B() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"DELETE {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        chat:partecipants chat:count ?oldValue .\r\n"
						+ "    }\r\n"
						+ "}\r\n"
						+ "INSERT {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        chat:partecipants chat:count ?newValue .\r\n"
						+ "        chat:partecipants rdf:value <"+receiver+"> .\r\n"
						+ "    }\r\n"
						+ "}\r\n"
						+ "WHERE {	\r\n"
						+ "    OPTIONAL{GRAPH <"+room+"> { chat:partecipants  chat:count  ?oldValue }} \r\n"
						+ "    BIND ((IF(BOUND(?oldValue), ?oldValue + 1, 1)) AS ?newValue )\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://wot.arces.unibo.it/chat/count",null));
		expectedAdded.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://www.w3.org/1999/02/22-rdf-syntax-ns#value",receiver));
		
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(room,"http://wot.arces.unibo.it/chat/partecipants","http://wot.arces.unibo.it/chat/count",null));
		
		check(sparqlUpdate,expectedAdded,expectedRemoved);

	}
	
	@Test
	public void TEST_06_SEND_A_to_B() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"INSERT {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        ?message rdf:type chat:type_text ;\r\n"
						+ "        chat:content \"msg 1\" ;\r\n"
						+ "        schema:sender <"+sender+"> ;\r\n"
						+ "        schema:toRecipient <"+receiver+"> ;\r\n"
						+ "        schema:dateSent ?time .\r\n"
						+ "    }\r\n"
						+ "} WHERE    {\r\n"
						//+ "    BIND(IRI(CONCAT(CONCAT(str(<"+room+">) ,\"/message_\"),STRUUID())) AS ?message) .\r\n"
						+ "    BIND(<"+msg+"> AS ?message) .\r\n"
						+ "    BIND(STR(now()) AS ?time) .\r\n"
						+ "    GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
						+ "        <"+sender+"> chat:status chat:registered .\r\n"
						+ "        <"+receiver+"> chat:status chat:registered .\r\n"
						+ "    }\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        chat:partecipants rdf:value <"+sender+"> .\r\n"
						+ "        chat:partecipants rdf:value <"+receiver+"> .\r\n"
						+ "    }\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(room,msg,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://wot.arces.unibo.it/chat/type_text"));
		expectedAdded.add(new TempQuadForTest(room,msg,"http://wot.arces.unibo.it/chat/content","msg 1"));
		expectedAdded.add(new TempQuadForTest(room,msg,"http://schema.org/sender",sender));
		expectedAdded.add(new TempQuadForTest(room,msg,"http://schema.org/toRecipient",receiver));
		expectedAdded.add(new TempQuadForTest(room,msg,"http://schema.org/dateSent",null));
	
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		
		//sub
		ArrayList<String> subHit = new ArrayList<String>();
		subHit.add(sub_receive_B);
		ArrayList<String> subNOTHit = new ArrayList<String>();
		subNOTHit.add(sub_room);
		subNOTHit.add(sub_cpu);
		subNOTHit.add(sub_log_deleted);
		subNOTHit.add(sub_log_send);
		subNOTHit.add(sub_log_received);
		
		check(sparqlUpdate,expectedAdded,expectedRemoved,subHit,subNOTHit);

	}
	
	@Test
	public void TEST_07_LOG_SEND() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"DELETE {\r\n"
						+ "					GRAPH <"+sender+"> {\r\n"
						+ "							chat:send chat:count ?oldValue .\r\n"
						+ "						}\r\n"
						+ "					}\r\n"
						+ "					INSERT {\r\n"
						+ "						GRAPH <"+sender+"> {\r\n"
						+ "							chat:send chat:count ?newValue .\r\n"
						+ "						}\r\n"
						+ "					}\r\n"
						+ "					WHERE {	\r\n"
						+ "						GRAPH <"+sender+"> {\r\n"
						+ "							chat:send chat:count ?oldValue .\r\n"
						+ "						}\r\n"
						+ "						BIND ((?oldValue + 1) AS ?newValue )\r\n"
						+ "					}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/send","http://wot.arces.unibo.it/chat/count",null));
	
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/send","http://wot.arces.unibo.it/chat/count",null));
		
		//sub
		ArrayList<String> subHit = new ArrayList<String>();
		subHit.add(sub_log_send);
		
		ArrayList<String> subNOTHit = new ArrayList<String>();
		subNOTHit.add(sub_room);
		subNOTHit.add(sub_cpu);
		subNOTHit.add(sub_receive_B);
		subNOTHit.add(sub_received_A);
		subNOTHit.add(sub_log_deleted);
		subNOTHit.add(sub_log_received);
		
		check(sparqlUpdate,expectedAdded,expectedRemoved,subHit,subNOTHit);

	}
	
	@Test
	public void TEST_08_SET_RECEIVED() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
						"INSERT DATA {\r\n"
						+ "    GRAPH <"+room+"> {\r\n"
						+ "        <"+msg+"> chat:received \"true\"^^xsd:boolean.\r\n"
						+ "    }\r\n"
						+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(room,msg,"http://wot.arces.unibo.it/chat/received",null));
	
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		
		//sub
		ArrayList<String> subHit = new ArrayList<String>();
		subHit.add(sub_received_A);
		
		ArrayList<String> subNOTHit = new ArrayList<String>();
		subNOTHit.add(sub_room);
		subNOTHit.add(sub_cpu);
		subNOTHit.add(sub_log_deleted);
		subNOTHit.add(sub_log_send);
		subNOTHit.add(sub_log_received);
		subNOTHit.add(sub_receive_B);
		
		check(sparqlUpdate,expectedAdded,expectedRemoved,subHit,subNOTHit);

	}
	
	@Test
	public void TEST_09_LOG_RECEIVED() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
				"DELETE {\r\n"
				+ "					GRAPH <"+receiver+"> {\r\n"
				+ "							chat:received chat:count ?oldValue .\r\n"
				+ "						}\r\n"
				+ "					}\r\n"
				+ "					INSERT {\r\n"
				+ "						GRAPH <"+receiver+"> {\r\n"
				+ "							chat:received chat:count ?newValue .\r\n"
				+ "						}\r\n"
				+ "					}\r\n"
				+ "					WHERE {	\r\n"
				+ "						GRAPH <"+receiver+"> {\r\n"
				+ "							chat:received chat:count ?oldValue .\r\n"
				+ "						}\r\n"
				+ "						BIND ((?oldValue + 1) AS ?newValue )\r\n"
				+ "					}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(receiver,"http://wot.arces.unibo.it/chat/received","http://wot.arces.unibo.it/chat/count",null));
		
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(receiver,"http://wot.arces.unibo.it/chat/received","http://wot.arces.unibo.it/chat/count",null));
		
		//sub
		ArrayList<String> subHit = new ArrayList<String>();
		subHit.add(sub_log_received);
		
		ArrayList<String> subNOTHit = new ArrayList<String>();
		subNOTHit.add(sub_room);
		subNOTHit.add(sub_cpu);
		subNOTHit.add(sub_log_deleted);
		subNOTHit.add(sub_log_send);
		subNOTHit.add(sub_receive_B);
		subNOTHit.add(sub_received_A);
		
		check(sparqlUpdate,expectedAdded,expectedRemoved,subHit,subNOTHit);

	}
	
	@Test
	public void TEST_10_DELETE_MSG() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
				"DELETE {\r\n"
				+ "    GRAPH <"+room+"> {\r\n"
				+ "        ?message ?p ?o.\r\n"
				+ "    }\r\n"
				+ "} WHERE    {\r\n"
				+ "   	GRAPH <"+room+"> {\r\n"
				+ "        ?message ?p ?o;\r\n"
				+ "        schema:sender <"+sender+"> ;\r\n"
				+ "        schema:toRecipient <"+receiver+">.\r\n"
				+ "    }\r\n"
				+ "    GRAPH <"+graphUri+"> {\r\n"
				+ "        <"+sender+"> chat:status chat:registered .\r\n"
				+ "        <"+receiver+"> chat:status chat:registered .\r\n"
				+ "    }\r\n"
				+ "    GRAPH <"+room+"> {\r\n"
				+ "        chat:partecipants rdf:value <"+sender+"> .\r\n"
				+ "        chat:partecipants rdf:value <"+receiver+"> .\r\n"
				+ "    }\r\n"
				+ "}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://schema.org/toRecipient",receiver));
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://wot.arces.unibo.it/chat/type_text"));
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://schema.org/sender",sender));
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://schema.org/dateSent",null));
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://wot.arces.unibo.it/chat/received",null));
		expectedRemoved.add(new TempQuadForTest(room,msg,"http://wot.arces.unibo.it/chat/content","msg 1"));
		
		check(sparqlUpdate,expectedAdded,expectedRemoved);

	}
	
	@Test
	public void TEST_11_LOG_DELETED() throws SEPASecurityException, SPARQL11ProtocolException, SEPASparqlParsingException {
		String sparqlUpdate = prefixs+
				"DELETE {\r\n"
				+ "					GRAPH <"+sender+"> {\r\n"
				+ "							chat:deleted chat:count ?oldValue .\r\n"
				+ "						}\r\n"
				+ "					}\r\n"
				+ "					INSERT {\r\n"
				+ "						GRAPH <"+sender+"> {\r\n"
				+ "							chat:deleted chat:count ?newValue .\r\n"
				+ "						}\r\n"
				+ "					}\r\n"
				+ "					WHERE {	\r\n"
				+ "						GRAPH <"+sender+"> {\r\n"
				+ "							chat:deleted chat:count ?oldValue .\r\n"
				+ "						}\r\n"
				+ "						BIND ((?oldValue + 1) AS ?newValue )\r\n"
				+ "					}";
		//EXPECTED
		Set<TempQuadForTest> expectedAdded = new HashSet<TempQuadForTest>();
		expectedAdded.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/deleted","http://wot.arces.unibo.it/chat/count",null));
		
		Set<TempQuadForTest> expectedRemoved = new HashSet<TempQuadForTest>();
		expectedRemoved.add(new TempQuadForTest(sender,"http://wot.arces.unibo.it/chat/deleted","http://wot.arces.unibo.it/chat/count",null));
		
		//sub
		ArrayList<String> subHit = new ArrayList<String>();
		subHit.add(sub_log_deleted);
		
		ArrayList<String> subNOTHit = new ArrayList<String>();
		subNOTHit.add(sub_room);
		subNOTHit.add(sub_cpu);
		subNOTHit.add(sub_log_received);
		subNOTHit.add(sub_log_send);
		subNOTHit.add(sub_receive_B);
		subNOTHit.add(sub_received_A);
		
		check(sparqlUpdate,expectedAdded,expectedRemoved);

	}
	
	private static void check(String sparqlUpdate,Set<TempQuadForTest> expectedAdded,Set<TempQuadForTest> expectedRemoved) throws SPARQL11ProtocolException, SEPASparqlParsingException
	{
		check(sparqlUpdate,expectedAdded,expectedRemoved,new ArrayList<String>(),new ArrayList<String>());
	}
	
	private static void check(String sparqlUpdate,Set<TempQuadForTest> expectedAdded,Set<TempQuadForTest> expectedRemoved,List<String> subscriptionHit,List<String> subscriptionNOTHit) throws SPARQL11ProtocolException, SEPASparqlParsingException{
		//###################ph1
		Response res1ph= store_ph_1.update(TestUtils.generateUpdate(sparqlUpdate));
		if(res1ph.isError()) {
			System.out.println(((ErrorResponse)res1ph).getErrorDescription());
			assertTrue(false);
		}else {
			UpdateResponseWithAR updateRes1ph = (UpdateResponseWithAR)res1ph;
			if(expectedAdded.size()>0) {
				assertTrue(TestUtils.quadsSetCompare(updateRes1ph.updatedTuples,expectedAdded,"ph1.added"));
			}else {
				assertTrue(updateRes1ph.updatedTuples.size()==0);
			}
			if(expectedRemoved.size()>0) {
				assertTrue(TestUtils.quadsSetCompare(updateRes1ph.removedTuples,expectedRemoved,"ph1.removed"));
			}else {
				assertTrue(updateRes1ph.removedTuples.size()==0);
			}
			
	
			//###################ph2
			InternalUpdateRequestWithQuads req2ph= ARQuadsAlgorithm.generateLUTTandInsertDelete(sparqlUpdate, updateRes1ph, null, null, null);
			assertTrue(req2ph.getResponseNothingToDo()==null && req2ph.getSparql().length()>0);
			
			Response res2ph= store_ph_2.update(TestUtils.generateUpdate(req2ph.getSparql()));
			if(res2ph.isError()) {
				System.out.println(((ErrorResponse)res2ph).getErrorDescription());
				assertTrue(false);
			}else {
				UpdateResponseWithAR updateRes2ph = (UpdateResponseWithAR)res2ph;
				if(expectedAdded.size()>0) {
					assertTrue(TestUtils.quadsSetCompare(updateRes2ph.updatedTuples,expectedAdded,"ph2.added"));
				}else {
					assertTrue(updateRes2ph.updatedTuples.size()==0);
				}
				if(expectedRemoved.size()>0) {
					assertTrue(TestUtils.quadsSetCompare(updateRes2ph.removedTuples,expectedRemoved,"ph2.removed"));
				}else {
					assertTrue(updateRes2ph.removedTuples.size()==0);
				}
				
				//###################LUTT HIT
				
				//GENERATE LUTT for 2ph too (this is not done on normal execution of 2ph-stores)
				LUTT LUTT2ph = ARQuadsAlgorithm.generateLUTTandInsertDelete(req2ph.getSparql(), updateRes2ph, null, null, null).getHitterLUTT();
				
				//get the normal lutt
				LUTT LUTT1ph =req2ph.getHitterLUTT();
				
				assertTrue(LUTT1ph.hit(LUTT2ph));
				
				//###################LUTT STRICT EQUALS
				

				assertTrue(LUTT1ph.getJollyGraph().size()==LUTT2ph.getJollyGraph().size());
				assertTrue(LUTT1ph.getLutt().size()==LUTT2ph.getLutt().size());
				
				check(LUTT1ph.getJollyGraph(),LUTT1ph.getJollyGraph());
				
				Iterator<String> keys = LUTT1ph.getLutt().keySet().iterator();
				while(keys.hasNext()) {
					String key= keys.next();
					assertTrue(LUTT2ph.getLutt().containsKey(key));
					check(LUTT1ph.getLutt().get(key),LUTT2ph.getLutt().get(key));
				}
				
				//###################subscription
				for(int x = 0;x<subscriptionHit.size();x++) {
					LUTT subLUTT = QueryLUTTextraction.exstract(subscriptionHit.get(x));
					assertTrue(subLUTT.hit(LUTT1ph));
				}
				
				for(int x = 0;x<subscriptionNOTHit.size();x++) {
					LUTT subLUTT = QueryLUTTextraction.exstract(subscriptionNOTHit.get(x));
					assertTrue(!subLUTT.hit(LUTT1ph));
				}
				
			}
			
		}
		
	}
	
	private static void check(ArrayList<LUTTTriple> a,ArrayList<LUTTTriple> b) {
		for(int x=0;x<a.size();x++) {
			LUTTTripleStrict luttstrict = new LUTTTripleStrict(a.get(x));
			assertTrue(luttstrict.isIn(b));
		}
	}
	


	
}



