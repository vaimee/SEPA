package it.unibo.arces.wot.sepa.engine.processing.lutt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class LUTTTest {
	
	

	@BeforeClass
	public static void init() throws SEPASecurityException {
	
	}


	

	@Test
	public void General_Test_01() throws SEPASecurityException {
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
	public void General_Test_02() throws SEPASecurityException {
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
	
	
}
