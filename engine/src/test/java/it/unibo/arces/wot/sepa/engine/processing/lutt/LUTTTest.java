package it.unibo.arces.wot.sepa.engine.processing.lutt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LUTTTest {
	
	

	@BeforeClass
	public static void init() throws SEPASecurityException {

	}


	@Test
	public void Test_01_General() throws SEPASecurityException {
		String sparqlQuery = 
					"PREFIX pp:<http://prefix.prova/> "
				+ 	"SELECT ?s ?p ?o WHERE {"
				+	"graph ?g { "
				+ 		" ?s pp:a \"prova\" ."
				+ 		" ?s ?p ?o ."
				+ 		" ?s <http://prefix.prova2> ?o2 ."
				+ 	"} }";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		List<LUTTTriple> triples = lutt.getJollyGraph();
		assertTrue(triples.size()==3);
		assertTrue(triples.contains(new LUTTTriple(null,"http://prefix.prova/a","prova")));
		assertTrue(triples.contains(new LUTTTriple(null,null,null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://prefix.prova2",null)));
	}
	
	@Test
	public void Test_02_General() throws SEPASecurityException {
		String sparqlQuery = 
					"PREFIX pp:<http://prefix.prova/> "
				+ 	"SELECT ?s ?p ?o WHERE {"
				+		"graph ?g { "
				+ 			" ?s pp:a ?o ."
				+ 		"}\n"
				+		"graph <http://myGraph> { "
				+ 			" ?s pp:a2 \"prova2\" ."
				+ 			" <http://mysubject> ?p ?o "
				+ 		"}\n"
				+		"graph <http://myGraph_2> { "
				+ 			" ?s ?p ?o ."
				+ 		"}\n"
				+ 	"}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		List<LUTTTriple> triples = lutt.getJollyGraph();
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(triples.size()==1);
		assertTrue(triples.contains(new LUTTTriple(null,"http://prefix.prova/a",null)));
		
		assertTrue(quads.keySet().size()==2);
		assertTrue(quads.keySet().contains("http://myGraph"));
		assertTrue(quads.keySet().contains("http://myGraph_2"));
		
		List<LUTTTriple> triples2= quads.get("http://myGraph");
		assertTrue(triples2.size()==2);
		assertTrue(triples2.contains(new LUTTTriple(null,"http://prefix.prova/a2","prova2")));
		assertTrue(triples2.contains(new LUTTTriple("http://mysubject",null,null)));
		
		List<LUTTTriple> triples3= quads.get("http://myGraph_2");
		assertTrue(triples3.size()==1);
		assertTrue(triples3.contains(new LUTTTriple(null,null,null)));
	}
	
	@Test
	public void Test_03_General() throws SEPASecurityException {
		String sparqlQuery = 
					"PREFIX xsd:<http://www.w3.org/2001/XMLSchema> "
				+	"PREFIX pp:<http://prefix.prova#> "
				+ 	"SELECT ?s WHERE {"
				+		"graph <http://myGraph> { "
				+ 			" ?s pp:a \"1\"^^xsd:integer ."
				+ 			" ?s ?p \"2022-02-28T14:45:00.000Z\"^^xsd:dateTime ."
				+ 		"}\n"
				+ 	"}";
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		assertTrue(quads.keySet().contains("http://myGraph"));
		
		List<LUTTTriple> triples= quads.get("http://myGraph");
		assertTrue(triples.size()==2);
		assertTrue(triples.contains(new LUTTTriple(null,"http://prefix.prova#a","1")));
		assertTrue(triples.contains(new LUTTTriple(null,null,"2022-02-28T14:45:00.000Z")));
		
	}
	
	@Test
	public void Test_04_Chat_sent() throws SEPASecurityException {
		String sparqlQuery = 
					"		PREFIX	schema: <http://schema.org/>\n"
					+ "		PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "		PREFIX 	chat: <http://wot.arces.unibo.it/chat#>\n"
					+ "			SELECT ?message ?sender ?name ?text ?time \n"
					+ "			FROM <http://wot.arces.unibo.it/chat/room_default> \n"
					+ "			WHERE { {\n"
					+ "					?message rdf:type schema:Message ;\n"
					+ "					schema:text ?text ;\n"
					+ "					schema:sender ?sender ;			\n"
					+ "					schema:dateSent ?time ;\n"
					+ "					chat:private \"1\";\n"
					+ "					schema:toRecipient chat:IAmAReceiver .\n"
					+ "					GRAPH <http://wot.arces.unibo.it/chat/> {\n"
					+ "									?sender rdf:type schema:Person ;\n"
					+ "									schema:name ?name .\n"
					+ "									chat:IAmAReceiver rdf:type schema:Person.\n"
					+ "					}	\n"
					+ "				}\n"
					+ "				UNION \n"
					+ "				{\n"
					+ "					?message rdf:type schema:Message ;\n"
					+ "					schema:text ?text ;\n"
					+ "					schema:sender ?sender ;			\n"
					+ "					schema:dateSent ?time ;\n"
					+ "					chat:private \"0\" .\n"
					+ "					GRAPH <http://wot.arces.unibo.it/chat/> {\n"
					+ "									?sender rdf:type schema:Person ;\n"
					+ "									schema:name ?name .\n"
					+ "					}	\n"
					+ "				} } ORDER BY ?time";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==2);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/room_default"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/room_default");
		assertTrue(triples.size()==11);
		assertTrue(triples.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message")));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/text",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/sender",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateSent",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://wot.arces.unibo.it/chat#private","1")));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/toRecipient","http://wot.arces.unibo.it/chat#IAmAReceiver")));
		//UNION
		assertTrue(triples.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message")));//duplicate
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/text",null)));//duplicate
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/sender",null)));//duplicate
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateSent",null)));//duplicate
		assertTrue(triples.contains(new LUTTTriple(null,"http://wot.arces.unibo.it/chat#private","0")));
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/"));
		List<LUTTTriple> triples2= quads.get("http://wot.arces.unibo.it/chat/");
		assertTrue(triples2.size()==5);
		assertTrue(triples2.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person")));
		assertTrue(triples2.contains(new LUTTTriple(null,"http://schema.org/name",null)));
		assertTrue(triples2.contains(new LUTTTriple("http://wot.arces.unibo.it/chat#IAmAReceiver","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person")));
		//UNION
		assertTrue(triples2.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person")));//duplicate
		assertTrue(triples2.contains(new LUTTTriple(null,"http://schema.org/name",null)));//duplicate
	
	}
	
	@Test
	public void Test_05_Chat_received() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
					+ "		PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "		PREFIX 	chat: <http://wot.arces.unibo.it/chat#>\n"
					+ "		SELECT ?receiver ?message ?time FROM <http://wot.arces.unibo.it/chat/room_default> WHERE {\r\n"
					+ "						?message schema:sender chat:IAmASender ;\r\n"
					+ "						schema:toRecipient ?receiver ;\r\n"
					+ "						schema:dateReceived ?time ;\r\n"
					+ "						rdf:type schema:Message .\r\n"
					+ "					}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/room_default"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/room_default");
		assertTrue(triples.size()==4);
		assertTrue(triples.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message")));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/toRecipient",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateReceived",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/sender","http://wot.arces.unibo.it/chat#IAmASender")));
		
	}
	
	
	@Test
	public void Test_06_Chat_received_monitor() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+ 				"PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ 				"PREFIX chat: <http://wot.arces.unibo.it/chat#>\n"
				+				"SELECT ?sender ?message ?time FROM <http://wot.arces.unibo.it/chat/> WHERE {"
				+ 						"?message schema:sender ?sender ; "
				+						"schema:dateReceived ?time ; "
				+ 						"rdf:type schema:Message"
				+ 				"}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/");
		assertTrue(triples.size()==3);
		assertTrue(triples.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Message")));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateReceived",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/sender",null)));
		
	}
	

	@Test
	public void Test_07_Chat_log_sent() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+ 				"PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ 				"PREFIX chat: <http://wot.arces.unibo.it/chat#>\n"
				+				"SELECT ?message ?sender ?receiver ?text ?dateSent FROM <http://wot.arces.unibo.it/chat/log> WHERE {"
				+ 						"?message schema:text ?text ; "
				+ 						"schema:sender ?sender ; "
				+ 						"schema:toRecipient ?receiver; "
				+ 						"schema:dateSent ?dateSent"
				+  				"}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/log"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/log");
		assertTrue(triples.size()==4);
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/text",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/sender",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/toRecipient",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateSent",null)));
	}
	
	@Test
	public void Test_08_Chat_log_received() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+				"SELECT ?message ?dateReceived FROM <http://wot.arces.unibo.it/chat/log> WHERE {"
				+ "?message schema:dateReceived ?dateReceived}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/log"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/log");
		assertTrue(triples.size()==1);
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/dateReceived",null)));
	}
	
	@Test
	public void Test_09_Chat_users() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+ 				"PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+				"SELECT ?user ?userName FROM <http://wot.arces.unibo.it/chat/> WHERE "
				+ "{?user rdf:type schema:Person ; schema:name ?userName}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==1);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/");
		assertTrue(triples.size()==2);
		assertTrue(triples.contains(new LUTTTriple(null,"http://schema.org/name",null)));
		assertTrue(triples.contains(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://schema.org/Person")));
	}
	
	@Test
	public void Test_10_Chat_query_all() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+ 				"PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+				"SELECT * FROM <http://wot.arces.unibo.it/chat/log> FROM <http://wot.arces.unibo.it/chat/> WHERE {?s ?p ?o}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		HashMap<String,ArrayList<LUTTTriple>> quads = lutt.getLutt();
		assertTrue(quads.keySet().size()==2);
		
		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/log"));
		List<LUTTTriple> triples= quads.get("http://wot.arces.unibo.it/chat/log");
		assertTrue(triples.size()==1);
		assertTrue(triples.contains(new LUTTTriple(null,null,null)));

		assertTrue(quads.keySet().contains("http://wot.arces.unibo.it/chat/"));
		triples= quads.get("http://wot.arces.unibo.it/chat/");
		assertTrue(triples.size()==1);
		assertTrue(triples.contains(new LUTTTriple(null,null,null)));
		
	}
	
	@Test
	public void Test_11_Hitter() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX	schema: <http://schema.org/>\n"
				+ 				"PREFIX	rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+				"SELECT ?user ?userName FROM <http://wot.arces.unibo.it/chat/> WHERE {?user rdf:type schema:Person ; schema:name ?userName}";
		
		LUTT luttQuery = QueryLUTTextraction.exstract(sparqlQuery);
		
		
		//################################################## HIT
		HashMap<String,ArrayList<LUTTTriple>> quads = new HashMap<String,ArrayList<LUTTTriple>>();
		ArrayList<LUTTTriple> triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#type", null));
		quads.put("http://wot.arces.unibo.it/chat/", triples_01);
		LUTT luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(luttAR.hit(luttQuery));
		assertTrue(luttQuery.hit(luttAR));
		
		
		
		//################################################## NOT HIT
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple(null,"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", null));
		quads.put("http://wot.arces.unibo.it/chat/", triples_01);
		luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(!luttAR.hit(luttQuery));
		assertTrue(!luttQuery.hit(luttAR));
		
		
		//################################################## HIT
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple("Andrea","http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://schema.org/Person"));
		quads.put("http://wot.arces.unibo.it/chat/", triples_01);
		luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(luttAR.hit(luttQuery));
		assertTrue(luttQuery.hit(luttAR));
		
		//################################################## NOT HIT
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple("SEPA","http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://schema.org/Product"));
		quads.put("http://wot.arces.unibo.it/chat/", triples_01);
		luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(!luttAR.hit(luttQuery));
		assertTrue(!luttQuery.hit(luttAR));
		
		//################################################## HIT
		/*
		 * The first LUTTTriple will not hit but the second LUTTTriple will do
		 */
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple("SEPA","http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://schema.org/Product"));
		triples_01.add(new LUTTTriple("SEPA","http://schema.org/name", null));
		quads.put("http://wot.arces.unibo.it/chat/", triples_01);
		luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(luttAR.hit(luttQuery));
		assertTrue(luttQuery.hit(luttAR));
		
		
		//################################################## NOT HIT
		/*
		 * The same as the previous test so should hit
		 * but the graph is wrong and so will not hit
		 */
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple("SEPA","http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://schema.org/Product"));
		triples_01.add(new LUTTTriple("SEPA","http://schema.org/name", null));
		quads.put("http://wot.arces.unibo.it/chat/wrong_graph", triples_01);
		luttAR = new LUTT(new ArrayList<LUTTTriple>(),quads);
		assertTrue(!luttAR.hit(luttQuery));
		assertTrue(!luttQuery.hit(luttAR));

		
		//################################################## HIT
		/*
		 * The same as the previous test so not hit for the wrong graph but we use 
		 * the same triple (that hit) in the jolly graph, result: hit
		 */
		quads = new HashMap<String,ArrayList<LUTTTriple>>();
		triples_01 = new ArrayList<LUTTTriple>();
		triples_01.add(new LUTTTriple("SEPA","http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://schema.org/Product"));
		triples_01.add(new LUTTTriple("SEPA","http://schema.org/name", null));
		quads.put("http://wot.arces.unibo.it/chat/wrong_graph", triples_01);
		luttAR = new LUTT(triples_01,quads);//<-------------------------------use "triples_01" as jolly graph too
		assertTrue(luttAR.hit(luttQuery));
		assertTrue(luttQuery.hit(luttAR));
		
		
	}
	
	@Test
	public void Test_11_final_chat() throws SEPASecurityException {
		String sparqlQuery = 	"PREFIX chat:<http://wot.arces.unibo.it/chat/>\r\n"
				+ "PREFIX schema:<http://schema.org/>\r\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\r\n"
				+ "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>\r\n"
				+				"SELECT ?person ?log ?count WHERE { \r\n"
				+ "				GRAPH <http://wot.arces.unibo.it/chat/> {\r\n"
				+ "					?person chat:status chat:registered.\r\n"
				+ "				} \r\n"
				+ "				GRAPH ?person {\r\n"
				+ "					?person rdf:type schema:Person.\r\n"
				+ "					chat:send chat:count ?count.\r\n"
				+ "				}\r\n"
				+ "			}";
		
		LUTT lutt = QueryLUTTextraction.exstract(sparqlQuery);
		assertTrue(lutt.getLutt().size()==1);
		assertTrue(lutt.getJollyGraph().size()==2);
		
			
	}
}
