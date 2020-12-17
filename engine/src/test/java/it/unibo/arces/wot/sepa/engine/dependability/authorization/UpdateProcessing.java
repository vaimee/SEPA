package it.unibo.arces.wot.sepa.engine.dependability.authorization;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.engine.processing.UpdateProcessor;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.engine.core.Engine;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalPreProcessedUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.EpSpecFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.updateprocessing.epspec.IEndPointSpecification;


public class UpdateProcessing {
	
	private final static String graph1 = "http://it.unibo.test.updateprocessing.1";
	private final static String graph2 = "http://it.unibo.test.updateprocessing.2";
	private final static ArrayList<InternalUpdateRequest> updates = new ArrayList<InternalUpdateRequest>();
	private final static ArrayList<ARBindingsResults> results = new ArrayList<ARBindingsResults>();
	private final static ArrayList<String> name = new ArrayList<String>();
	private static IEndPointSpecification eps;
	private static Engine engine;

	
	
	@BeforeClass
	public static void init() {
		System.out.println("Junit UpdateProcessing-> Starting sepa") ;
		engine =new Engine(new String[0]);		
		cleanKB();
		System.out.println("Junit UpdateProcessing-> Starting test");
		eps = EpSpecFactory.getInstance();
		//create tests single graph
		initUpdateDataInsert();
		initUpdateDeleteWhere();
		initUpdateModify();
		initUpdateDataDelete();
		initUpdateModifyWhere();
		//create tests on 2 graphs
		initUpdateDataInsert_2g();
		initUpdateDeleteWhere_2g();
		initUpdateModify_2g();
		initUpdateDataDelete_2g();
		initUpdateModifyWhere_2g();
		
	}
	
	@After
	public void clean() {
		cleanKB();
		boolean shutdownPass = true;
		try {
			engine.shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Junit UpdateProcessing-> Engine shutdown error: "+ e.getMessage());
			shutdownPass = false;
		}
		assertTrue("Engine shutdown",shutdownPass);
	}
	
	@Test
	public void updatesTest() {
		// the execution order of these test is important
		for (int x=0;x<name.size() ;x++) {
			System.out.println("Junit UpdateProcessing-> Start test "+ name.get(x));
			boolean passed= false;
			String resultInsertDeleteUpdate=null;
			try {
				InternalPreProcessedUpdateRequest ippur = new InternalPreProcessedUpdateRequest(updates.get(x));
				passed=areEquals(ippur.getARBindingsResults(),results.get(x));
				resultInsertDeleteUpdate=ippur.getSparql();
			}catch (Exception e) {
				System.out.println("Junit UpdateProcessing-> Test "+name.get(x)+ ", error: "+ e.getMessage());
				passed=false;
			}
			assertTrue("Test "+ name.get(x),passed);
			//
			if(resultInsertDeleteUpdate!=null) {
				assertFalse("InsertDeleteUpdate of test "+ name.get(x),executeUpdate(resultInsertDeleteUpdate));
			}
		}
	
	
	}
	
	//----------------------------------------------------
	//-----------------------------Test generators 1 graph
	//----------------------------------------------------

	
	private static void initUpdateDataInsert() {
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
		
	private static void initUpdateDeleteWhere() {
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
		
	private static void initUpdateModify() {
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
	
	private static void initUpdateDataDelete() {
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
	
	private static void initUpdateModifyWhere() {
		String testName = "UpdateModifyWhere";
		String sparql = ""+
				"DELETE {  \r\n" + 
				"		GRAPH <"+graph1+">  {\r\n" + 
				"			?s ?p ?o\r\n" + 
				"		} \r\n" + 
				"	} INSERT {  \r\n" + 
				"		GRAPH <"+graph1+"> {\r\n" + 
				"			<http://this> <http://type> <http://UpdateModifyWhere> ;\r\n" + 
				"			<http://label> \"This is the last test on single graph\" ; \r\n" + 
				"			<http://where> <http://not.void> .\r\n" + 
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
	
	private static void initUpdateDataInsert_2g() {
		String testName = "UpdateDataInsert";
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
		
	private static void initUpdateDeleteWhere_2g() {
		String testName = "UpdateDeleteWhere";
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
		
	private static void initUpdateModify_2g() {
		String testName = "UpdateModify";
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
	
	private static void initUpdateDataDelete_2g() {
		String testName = "UpdateDataDelete";
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
	
	
	private static void initUpdateModifyWhere_2g() {
		String testName = "UpdateModifyWhere";
		String sparql = ""+
				"DELETE {  \r\n" + 
				"		GRAPH <"+graph1+">  {\r\n" + 
				"			?s ?p ?o\r\n" + 
				"		} \r\n" + 
				"	} INSERT {  \r\n" + 
				"		GRAPH <"+graph2+"> {\r\n" + 
				"			<http://this> <http://type> <http://UpdateModifyWhere.g2> ;\r\n" + 
				"			<http://label> \"This is the last test on 2 graphs\" ; \r\n" + 
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
	
	private static boolean executeUpdate(String sparql) {
		try {
			UpdateProcessor processor =new UpdateProcessor(new SPARQL11Properties("endpoint.jpar"));
			return processor.process(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization())).isError();
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SPARQL11ProtocolException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
	}
	
	
	private static void cleanKB() {
		System.out.println("Junit UpdateProcessing-> Clean graphs: <"+ graph1 + ">; <"+ graph2+">");
		String sparql ="CLEAR GRAPH <"+ graph1 + ">;"+
				"CLEAR GRAPH <"+ graph2 + ">;";
		try {
			UpdateProcessor processor =new UpdateProcessor(new SPARQL11Properties("endpoint.jpar"));
			System.out.println("Junit UpdateProcessing-> Clean error: "+processor.process(new InternalUpdateRequest(sparql,new HashSet<String>(),new HashSet<String>(), new ClientAuthorization())).isError());
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SPARQL11ProtocolException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
}
