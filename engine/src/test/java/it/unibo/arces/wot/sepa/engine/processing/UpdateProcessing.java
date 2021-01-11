package it.unibo.arces.wot.sepa.engine.processing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.engine.dependability.authorization.ConfigurationProvider;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.processing.epspec.EpSpecFactory.EndPointSpec;
import it.unibo.arces.wot.sepa.engine.processing.epspec.IEndPointSpecification;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;


public class UpdateProcessing {


	
private final static String graph1 = "http://it.unibo.test.updateprocessing.1";
	private final static String graph2 = "http://it.unibo.test.updateprocessing.2";
	private final static ArrayList<InternalUpdateRequest> updates = new ArrayList<InternalUpdateRequest>();
	private final static ArrayList<ARBindingsResults> results = new ArrayList<ARBindingsResults>();
	private final static ArrayList<String> name = new ArrayList<String>();
	private static IEndPointSpecification eps;
	private static QueryProcessor queryProcessor;
	private static UpdateProcessor updateProcessor;
	private static boolean testAbort =false;
	private static boolean verbose = true;
			
	
	
	@BeforeClass
	public static void init() throws SEPASecurityException {		
		System.out.println("[Junit][ARQuadsAlgorith] Prepare test");
		String endpointJpar =new ConfigurationProvider2().a;
		if(verbose) {
			System.out.println("[VERBOSE] endpointJpar percorso: "+ endpointJpar);
		}
		EpSpecFactory.setInstanceFromFile(endpointJpar);
		eps = EpSpecFactory.getInstance();		
		System.out.println("[Junit][ARQuadsAlgorith] end point: "+ eps.getEndPointName().toString());
		try {
			SPARQL11Properties props = new SPARQL11Properties(endpointJpar);
			queryProcessor = new QueryProcessor(props);
			updateProcessor= new UpdateProcessor(props);	
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e1) {
			testAbort=true;
			e1.printStackTrace();
		}
	
		
		if(!testAbort) {
			try {
				testAbort=cleanKB();
			} catch (SPARQL11ProtocolException | SEPASparqlParsingException | SEPASecurityException | IOException e2) {
				testAbort=true;
				e2.printStackTrace();
			}
			
		}
	
		if(!testAbort) {
			try {
				//create tests single graph
				initUpdateDataInsert();
				initUpdateDeleteWhere();
				initUpdateModify();
				initUpdateDataDelete();
				if(eps.getEndPointName()!=EndPointSpec.BLAZEGRAPH) {
					initUpdateModifyWhere();
				}else {
					System.out.println("UpdateModifyWhere SKIPPED (don't work on blazegraph)");
				}
				//create tests on 2 graphs
				initUpdateDataInsert_2g();
				initUpdateDeleteWhere_2g();
				initUpdateModify_2g();
				initUpdateDataDelete_2g();
				if(eps.getEndPointName()!=EndPointSpec.BLAZEGRAPH) {
					initUpdateModifyWhere_2g();
				}else {
					System.out.println("initUpdateModifyWhere_2g SKIPPED (don't work on blazegraph)");
				}
			} catch (SPARQL11ProtocolException | SEPASparqlParsingException e) {
				testAbort=true;
				e.printStackTrace();
			}
		
		}
		
		
	}
	
	@After
	public void clean() {
		
		try {
			cleanKB();
		} catch (SPARQL11ProtocolException | SEPASparqlParsingException | SEPASecurityException | IOException e) {
	
			e.printStackTrace();
		}

	}
	
	@Test
	public void updatesTest() {		
		myAssertFalse("Preparation test",testAbort);
		// the execution order of these test is important
		for (int x=0;x<name.size() ;x++) {
			System.out.println("Junit UpdateProcessing-> Start test "+ name.get(x));
			boolean passed= false;
			String resultInsertDeleteUpdate=null;
			try {
				ARQuadsAlgorithm arqa = new ARQuadsAlgorithm(updates.get(x), queryProcessor);
				InternalUpdateRequestWithQuads ippur= arqa.extractARQuads();
				passed=areEquals(ippur.getARBindingsResults(),results.get(x));
				resultInsertDeleteUpdate=ippur.getSparql();
			}catch (Exception e) {
				System.out.println("Junit UpdateProcessing-> Test "+name.get(x)+ ", error: "+ e.getMessage());
				passed=false;
			}
			myAssertTrue("Test "+ name.get(x),passed);
			//
			if(resultInsertDeleteUpdate!=null) {
				try {
					myAssertFalse("InsertDeleteUpdate of test "+ name.get(x),executeUpdate(resultInsertDeleteUpdate));
					
				} catch (SEPASparqlParsingException e) {
					e.printStackTrace();
					myAssertFalse("InsertDeleteUpdate of test "+ name.get(x),false);
				}
			}
		}
	
	
	}
	
	//----------------------------------------------------
	//-----------------------------Test generators 1 graph
	//----------------------------------------------------

	
	private static void initUpdateDataInsert() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDataInsert";
		String sparql = ""+
				"INSERT DATA{\r\n" + 
				"	GRAPH <"+graph1+"> {\r\n" + 
				"			<http://sepa>	<http://is>		<http://awesome> .\r\n" + 
				"			<http://this>	<http://is>		<http://test> .\r\n" + 
				"			<http://test>	<http://label> 	\"Update Processing Test\" \r\n" + 
				"					 }\r\n" + 
				"}";
		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph1,"http://sepa", "http://is", "http://awesome",false));
		added.add(generateBindingsTriple(graph1,"http://this", "http://is", "http://test",false));
		added.add(generateBindingsTriple(graph1,"http://test", "http://label", "Update Processing Test",true));		
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
		
	private static void initUpdateDeleteWhere() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDeleteWhere";
		String sparql = ""+
				"DELETE WHERE {\r\n" + 
				"		GRAPH <"+graph1+"> { \r\n" + 
				"			?s <http://is> ?o\r\n" + 
				"		}\r\n" + 
				"}";

		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://sepa", "http://is", "http://awesome",false));
		removed.add(generateBindingsTriple(graph1,"http://this", "http://is", "http://test",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
		
	private static void initUpdateModify() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateModify";
		String sparql = ""+
				"DELETE {  GRAPH <"+graph1+">  {\r\n" + 
				"			<http://test>	<http://label> 	\"Update Processing Test\" \r\n" + 
				"} } INSERT {  GRAPH <"+graph1+"> {\r\n" + 
				"				<http://test>	<http://label> \"Another test :)\" . \r\n" + 
				"				<http://test>	<http://type> 	<http://UpdateModify> . \r\n" + 
				"				<http://test>	<http://where> 	<http://void> . \r\n" + 
				"} }	WHERE {}";

		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph1,"http://test", "http://label", "Another test :)",true));
		added.add(generateBindingsTriple(graph1,"http://test", "http://type", "http://UpdateModify",false));
		added.add(generateBindingsTriple(graph1,"http://test", "http://where", "http://void",false));
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test", "http://label", "Update Processing Test",true));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	
	private static void initUpdateDataDelete() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDataDelete";
		String sparql = ""+
				"DELETE DATA { GRAPH  <"+graph1+"> { \r\n" + 
				"			<http://test>	<http://type> <http://UpdateModify> \r\n" + 
				"} }\r\n";
	
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test", "http://type", "http://UpdateModify",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	
	private static void initUpdateModifyWhere() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateModifyWhere";
		String sparql = ""+
				"DELETE {  \r\n" + 
				"		GRAPH <"+graph1+">  {\r\n" + 
				"			?s ?p ?o\r\n" + 
				"		} \r\n" + 
				"	} INSERT {  \r\n" + 
				"		GRAPH <"+graph1+"> {\r\n" + 
				"			<http://this> <http://type> <http://UpdateModifyWhere> .\r\n" + 
				"			<http://this> <http://label> \"This is the last test on single graph\" .\r\n" + 
				"			<http://this> <http://where> <http://not.void> .\r\n" + 
				"		} \r\n" + 
				"	} WHERE {\r\n" + 
				"		?s ?p ?o .\r\n" + 
				"		<http://test> ?p ?o\r\n" + 
				"}";
	
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph1,"http://this", "http://type", "http://UpdateModifyWhere",false));
		added.add(generateBindingsTriple(graph1,"http://this", "http://where", "http://not.void",false));
		added.add(generateBindingsTriple(graph1,"http://this", "http://label", "This is the last test on single graph",true));
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test", "http://label", "Another test :)",true));
		removed.add(generateBindingsTriple(graph1,"http://test", "http://where", "http://void",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	
	
	//----------------------------------------------------
	//----------------------------Test generators 2 graphs
	//----------------------------------------------------
	
	private static void initUpdateDataInsert_2g() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDataInsert_2g";
		String sparql = ""+
				"INSERT DATA{\r\n" + 
				"	GRAPH <"+graph1+"> {\r\n" + 
				"			<http://sepa>	<http://is>		<http://awesome> .\r\n" + 
				"			<http://this>	<http://is>		<http://test.1> .\r\n" + 
				"			<http://test>	<http://label> 	\"Update Processing Test\" \r\n" + 
				"	}\r\n" + 
				"	GRAPH <"+graph2+"> {\r\n" + 
				"			<http://sepa>	<http://is>		<http://awesome> .\r\n" + 
				"			<http://this>	<http://is>		<http://test.2> .\r\n" + 
				"			<http://test>	<http://label> 	\"Test on 2 graph\" \r\n" + 
				"	}\r\n" + 
				"}";
		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph1,"http://sepa", "http://is", "http://awesome",false));
		added.add(generateBindingsTriple(graph1,"http://this", "http://is", "http://test.1",false));
		added.add(generateBindingsTriple(graph1,"http://test", "http://label", "Update Processing Test",true));	
		added.add(generateBindingsTriple(graph2,"http://sepa", "http://is", "http://awesome",false));
		added.add(generateBindingsTriple(graph2,"http://this", "http://is", "http://test.2",false));
		added.add(generateBindingsTriple(graph2,"http://test", "http://label", "Test on 2 graph",true));		
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
		
	private static void initUpdateDeleteWhere_2g() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDeleteWhere_2g";
		String sparql = ""+
				"DELETE WHERE {\r\n" + 
				"		GRAPH <"+graph1+"> { \r\n" + 
				"			?s <http://is> ?o\r\n" + 
				"		}\r\n" + 
				"		GRAPH <"+graph2+"> { \r\n" + 
				"			?s <http://is> ?o\r\n" + 
				"		}\r\n" + 
				"}";

		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://sepa", "http://is", "http://awesome",false));
		removed.add(generateBindingsTriple(graph1,"http://this", "http://is", "http://test.1",false));	
		removed.add(generateBindingsTriple(graph2,"http://sepa", "http://is", "http://awesome",false));
		removed.add(generateBindingsTriple(graph2,"http://this", "http://is", "http://test.2",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
		
	private static void initUpdateModify_2g() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateModify_2g";
		String sparql = ""+
				"DELETE { "+
					"GRAPH <"+graph1+">  {\r\n" + 
					"			<http://test>	<http://label> 	\"Update Processing Test\" \r\n" + 
					"} "+ 
					"GRAPH <"+graph2+">  {\r\n" + 
					"			<http://test>	<http://label> 	\"Test on 2 graph\" \r\n" + 
					"} "+ 
				"} INSERT { "+ 
					" GRAPH <"+graph1+"> {\r\n" + 
					"				<http://test.g1>	<http://label> \"Another test :)\" . \r\n" + 
					"				<http://test.g1>	<http://type> 	<http://UpdateModify.g1> . \r\n" + 
					"				<http://test.g1>	<http://where> 	<http://void> . \r\n" + 
					"}"+
					" GRAPH <"+graph2+"> {\r\n" + 
					"				<http://test.g2>	<http://label> \"Another test :)\" . \r\n" + 
					"				<http://test.g2>	<http://type> 	<http://UpdateModify.g2> . \r\n" + 
					"				<http://test.g2>	<http://where> 	<http://void> . \r\n" + 
					"}"+
				" }	WHERE {}";

		
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph1,"http://test.g1", "http://label", "Another test :)",true));
		added.add(generateBindingsTriple(graph1,"http://test.g1", "http://type", "http://UpdateModify.g1",false));
		added.add(generateBindingsTriple(graph1,"http://test.g1", "http://where", "http://void",false));
		added.add(generateBindingsTriple(graph2,"http://test.g2", "http://label", "Another test :)",true));
		added.add(generateBindingsTriple(graph2,"http://test.g2", "http://type", "http://UpdateModify.g2",false));
		added.add(generateBindingsTriple(graph2,"http://test.g2", "http://where", "http://void",false));
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test", "http://label", "Update Processing Test",true));
		removed.add(generateBindingsTriple(graph2,"http://test", "http://label", "Test on 2 graph",true));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	
	private static void initUpdateDataDelete_2g() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateDataDelete_2g";
		String sparql = ""+
				"DELETE DATA { "+ 
				"GRAPH  <"+graph1+"> { \r\n" + 
				"			<http://test.g1>	<http://type> <http://UpdateModify.g1> \r\n" + 
				"} "+ 
				"GRAPH  <"+graph2+"> { \r\n" + 
				"			<http://test.g2>	<http://type> <http://UpdateModify.g2> \r\n" + 
				"} "+ 
				"}\r\n";
	
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test.g1", "http://type", "http://UpdateModify.g1",false));
		removed.add(generateBindingsTriple(graph2,"http://test.g2", "http://type", "http://UpdateModify.g2",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	
	
	private static void initUpdateModifyWhere_2g() throws SPARQL11ProtocolException, SEPASparqlParsingException {
		String testName = "UpdateModifyWhere_2g";
		String sparql = ""+
				"DELETE {  \r\n" + 
				"		GRAPH <"+graph1+">  {\r\n" + 
				"			?s ?p ?o\r\n" + 
				"		} \r\n" + 
				"	} INSERT {  \r\n" + 
				"		GRAPH <"+graph2+"> {\r\n" + 
				"			<http://this> <http://type> <http://UpdateModifyWhere.g2> ;\r\n" + 
				"			<http://label> \"This is the last test on 2 graphs\" . \r\n" + 
				"		} \r\n" + 
				"	} WHERE {\r\n" + 
				"		?s ?p ?o .\r\n" + 
				"		<http://test.g1> ?p ?o\r\n" + 
				"}";
	
		ArrayList<Bindings> added = new ArrayList<Bindings>();
		added.add(generateBindingsTriple(graph2,"http://this", "http://type", "http://UpdateModifyWhere.g2",false));
		added.add(generateBindingsTriple(graph2,"http://this", "http://label", "This is the last test on 2 graphs",true));
		ArrayList<Bindings> removed = new ArrayList<Bindings>();
		removed.add(generateBindingsTriple(graph1,"http://test.g1", "http://label", "Another test :)",true));
		removed.add(generateBindingsTriple(graph1,"http://test.g1", "http://where", "http://void",false));
		ArrayList<String>  vars =eps.vars();
		vars.add(eps.g());
		ARBindingsResults expected = new ARBindingsResults(new BindingsResults(vars, added),new BindingsResults(vars, removed));
		
		updates.add(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		results.add(expected);
		name.add(testName);
	}
	

	
	//----------------------------------------------------
	//-----------------------------------------------UTILS
	//----------------------------------------------------
	
	private static boolean executeUpdate(String sparql) throws SEPASparqlParsingException {
		try {
			return updateProcessor.process(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization())).isError();
		} catch (SEPASecurityException | SPARQL11ProtocolException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}
	
	
	private static boolean cleanKB() throws SEPASparqlParsingException, SPARQL11ProtocolException, SEPASecurityException, IOException {
		if(verbose ) {	
			System.out.println("[VERBOSE] Clean graphs: <"+ graph1 + ">; <"+ graph2+">");
		}
	
		String sparql ="CLEAR GRAPH <"+ graph1 + ">;"+
				"CLEAR GRAPH <"+ graph2 + ">;";
		Response resp = updateProcessor.process(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization()));
		boolean error = resp.isError();
		if(verbose && error) {	
			System.out.println("[VERBOSE] errore clean: "+ resp.toString());
		}
		System.out.println("Junit UpdateProcessing-> Clean error: "+error);
		return error;
		
	}
	
	private static Bindings generateBindingsTriple(String g, String s, String p,String o, boolean pAsLiteral) {
		Bindings t = new Bindings();	
		t.addBinding(eps.g(), new RDFTermURI(g));
		t.addBinding(eps.s(), new RDFTermURI(s));
		t.addBinding(eps.p(), new RDFTermURI(p));
		if(pAsLiteral) {
			t.addBinding(eps.o(), new RDFTermLiteral(o));	
		}else {
			t.addBinding(eps.o(), new RDFTermURI(o));				
		}
		return t;
	}
	
	private static boolean areEquals(ARBindingsResults a, ARBindingsResults b) {
		if( a.getAddedBindings().size() == b.getAddedBindings().size() && a.getRemovedBindings().size() == b.getRemovedBindings().size()) {
			for (Bindings a_binds : a.getAddedBindings().getBindings()) {
				if(!b.getAddedBindings().contains(a_binds)) {
					return false;
				}
			}
			for (Bindings b_binds : b.getAddedBindings().getBindings()) {
				if(!a.getAddedBindings().contains(b_binds)) {
					return false;
				}
			}
			for (Bindings a_binds : a.getRemovedBindings().getBindings()) {
				if(!b.getRemovedBindings().contains(a_binds)) {
					return false;
				}
			}
			for (Bindings b_binds : b.getRemovedBindings().getBindings()) {
				if(!a.getRemovedBindings().contains(b_binds)) {
					return false;
				}
			}
			return true;
		}else {
			return false;
		}
		
	}
	
	private static void myAssertFalse(String text, boolean ris) {
		if(ris) {
			System.out.println("assert NOT PASS: "+ text);
		}
		assertFalse(text,ris);
	}
	
	private static void myAssertTrue(String text, boolean ris) {
		if(!ris) {
			System.out.println("assert NOT PASS: "+ text);
		}
		assertTrue(text,ris);
	}
}
