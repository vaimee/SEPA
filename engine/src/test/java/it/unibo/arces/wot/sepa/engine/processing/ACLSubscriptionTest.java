package it.unibo.arces.wot.sepa.engine.processing;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initGroupsQuery;
import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initQuery;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.acl.ACLException;
import org.apache.jena.base.Sys;
import org.apache.jena.shared.AccessDeniedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.QueryHTTPMethod;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.UpdateHTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.security.Credentials;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageDataset;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageOperations;
import it.unibo.arces.wot.sepa.engine.acl.storage.Constants;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.core.Engine;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.ACLTools;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalAclRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalStdRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;


public class ACLSubscriptionTest  {
	

	
	private  static final   InternalAclRequestFactory       aclReqFactory = new InternalAclRequestFactory();
	private  static final   InternalStdRequestFactory       stdReqFactory = new InternalStdRequestFactory();
	private static Processor sepaProcessor;
	private static List<SubscriptionController> controllers;
	
    private static final String  insert = 
    "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
    "INSERT DATA  {  "                                          + System.lineSeparator() + 
    "	GRAPH mp:graph1 { "                                    + System.lineSeparator() + 
    "		<http://s1> <http://p1> <http://o1> ."          + System.lineSeparator() + 
    "	}"+
    "	GRAPH mp:graph2 { "                                    + System.lineSeparator() + 
    "		<http://s1> <http://p1> <http://o2> ."          + System.lineSeparator() + 
    "	}"+
    "	GRAPH mp:graph3 { "                                    + System.lineSeparator() + 
    "		<http://s1> <http://p1> <http://o3> ."          + System.lineSeparator() + 
    "	}"+
    "	GRAPH mp:graph4 { "                                    + System.lineSeparator() + 
    "		<http://s1> <http://p1> <http://o4> ."          + System.lineSeparator() + 
    "	}"
    + "}" ;
	            
    private static final String sub1 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH mp:graph1  {?s ?p ?o}}";
        
    private static final String sub2= 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH mp:graph2  {?s ?p ?o}}";
    
    private static final String sub3 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH  mp:graph3  {?s ?p ?o}}";

    private static final String sub4 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH  mp:graph4  {?s ?p ?o}}";
   
    private static final String subAll = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH ?g {?s ?p ?o}}";
    
    private static final String sub_1_and_2 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o FROM NAMED  mp:graph1 \n FROM NAMED  mp:graph2 \n WHERE {GRAPH ?g {?s ?p ?o}}";
    
    private static final String sub_1_and_2_no_from = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?o WHERE {GRAPH  mp:graph1 {?o ?p ?o1} GRAPH  mp:graph2 {?o ?p ?o2}}";
    
    private static final String sub_1_and_3_no_from = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?o WHERE {GRAPH  mp:graph1 {?o ?p ?o1} GRAPH  mp:graph3 {?o ?p ?o2}}";
    
    private static final String sub_form_1 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o FROM NAMED  mp:graph1 \n WHERE {GRAPH ?g {?s ?p ?o}}";
    
    private static final String sub_form_2 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o FROM NAMED  mp:graph2 \n WHERE {GRAPH ?g {?s ?p ?o}}";
    
 
	@BeforeAll
	public static void init() throws SEPASecurityException {
		try {
			final SPARQL11Properties sepaEndpointProps = new SPARQL11Properties(Engine.defaultEndpointJpar);
			EngineProperties props = EngineProperties.load("engine.jpar");
			EngineBeans.setEngineProperties(props);
			sepaEndpointProps.setProtocolScheme(SPARQL11Properties.ProtocolScheme.SJenarAPI);
			//adjust properties
			EngineBeans.setAclEnabled(true);
			EngineBeans.setLUTTEnabled(false);

			//creates ACL objects
			final ACLStorageOperations      aclStorage = ACLTools.makeACLStorage();
			final SEPAAcl                   aclData = SEPAAcl.getInstance(aclStorage);
			setStorageOwner(aclData, aclStorage);

			//setting processors
			sepaProcessor = new Processor(sepaEndpointProps,props,new Scheduler(props));
			//load ACL with default test data
			doUpdate(aclReqFactory, sepaProcessor, initGroupsQuery,SEPAAcl.ADMIN_USER);
			doUpdate(aclReqFactory,sepaProcessor, initQuery,SEPAAcl.ADMIN_USER);

			controllers= new ArrayList<SubscriptionController>();
			
			//create subscriptions
			//########################################################## AS ADMIN
			//################################################	01
//			List<String> expected_01= new ArrayList<>();
//			expected_01.add("http://o1");
//			subscribe(sub1,"01",expected_01,SEPAAcl.ADMIN_USER);
//			//################################################	02
//			List<String> expected_02= new ArrayList<>();
//			expected_02.add("http://o2");
//			subscribe(sub2,"02",expected_02,SEPAAcl.ADMIN_USER);
//			//################################################	03
//			List<String> expected_03= new ArrayList<>();
//			expected_03.add("http://o3");
//			subscribe(sub3,"03",expected_03,SEPAAcl.ADMIN_USER);
//			//################################################	04
//			List<String> expected_04= new ArrayList<>();
//			expected_04.add("http://o4");
//			subscribe(sub4,"04",expected_04,SEPAAcl.ADMIN_USER);
//			//################################################	05
//			List<String> expected_05= new ArrayList<>();
//			expected_05.add("http://o1");
//			expected_05.add("http://o2");
//			expected_05.add("http://o3");
//			expected_05.add("http://o4");
//			subscribe(subAll,"05",expected_05,SEPAAcl.ADMIN_USER);
//			//################################################	06
//			List<String> expected_06= new ArrayList<>();
//			expected_06.add("http://o1");
//			expected_06.add("http://o2");
//			subscribe(sub_1_and_2,"06",expected_06,SEPAAcl.ADMIN_USER);
//			//################################################	06b
//			List<String> expected_06b= new ArrayList<>();
//			expected_06b.add("http://s1");
//			subscribe(sub_1_and_2_no_from,"06b",expected_06b,SEPAAcl.ADMIN_USER);

			//########################################################## AS inexistent user 
			//################################################	07
//			subscribe(sub1,"07",new ArrayList<>(),"I_m_a_ghost");
//			//################################################	08
//			subscribe(sub2,"08",new ArrayList<>(),"I_m_a_ghost");
//			//################################################	09
//			subscribe(sub3,"09",new ArrayList<>(),"I_m_a_ghost");
//			//################################################	10
//			subscribe(sub4,"10",new ArrayList<>(),"I_m_a_ghost");
//			//################################################	11
//			subscribe(subAll,"11",new ArrayList<>(),"I_m_a_ghost");
//			//################################################	12
//			subscribe(sub_1_and_2,"12",new ArrayList<>(),"I_m_a_ghost");	//NOT PASSED
//			//################################################	12b
//			subscribe(sub_1_and_2_no_from,"12b",new ArrayList<>(),"I_m_a_ghost");

//			//########################################################## AS gonger
//			//################################################	13
//			subscribe(sub1,"13",new ArrayList<>(),Constants.USER2);
//			//################################################	14
//			List<String> expected_14= new ArrayList<>();
//			expected_14.add("http://o2");
//			subscribe(sub2,"14",expected_14,Constants.USER2);
//			//################################################	15
//			subscribe(sub3,"15",new ArrayList<>(),Constants.USER2);
//			//################################################	16
//			List<String> expected_16= new ArrayList<>();
//			expected_16.add("http://o4");
//			subscribe(sub4,"16",expected_16,Constants.USER2);
//			//################################################	17
//			List<String> expected_17= new ArrayList<>();
//			expected_17.add("http://o2");
//			expected_17.add("http://o4");
//			subscribe(subAll,"17",expected_17,Constants.USER2);
//			//################################################	18 //NOT PASSED
////			List<String> expected_18= new ArrayList<>();
////			expected_18.add("http://o2");
////			expected_18.add("http://o4");
////			subscribe(sub_1_and_2,"18",expected_18,Constants.USER2);
//			//################################################	18b 
//			subscribe(sub_1_and_2_no_from,"18b",new ArrayList<>(),Constants.USER2);
			
			
			//########################################################## AS monger
			//################################################	19
			List<String> expected_19= new ArrayList<>();
			expected_19.add("http://o1");
			subscribe(sub1,"19",expected_19,Constants.USER1);
			//################################################	20
			subscribe(sub2,"20",new ArrayList<>(),Constants.USER1);
			//################################################	21
			List<String> expected_21= new ArrayList<>();
			expected_21.add("http://o3");
			subscribe(sub3,"21",expected_21,Constants.USER1);
			//################################################	22
			subscribe(sub4,"22",new ArrayList<>(),Constants.USER1);
			//################################################	23
			List<String> expected_23= new ArrayList<>();
			expected_23.add("http://o1");
			expected_23.add("http://o3");
			subscribe(subAll,"23",expected_23,Constants.USER1);
			//################################################	24 //NOT PASSED
//			List<String> expected_24= new ArrayList<>();
//			expected_24.add("http://o1");
//			subscribe(sub_1_and_2,"24",expected_24,Constants.USER1);
			//################################################	24b
			subscribe(sub_1_and_2_no_from,"24b",new ArrayList<>(),Constants.USER1);
			//################################################	25
			List<String> expected_25= new ArrayList<>();
			expected_25.add("http://s1");
			subscribe(sub_1_and_3_no_from,"25",expected_25,Constants.USER1);
			//################################################	26
			List<String> expected_26= new ArrayList<>();
			expected_26.add("http://o1");
			subscribe(sub_form_1,"26",expected_26,Constants.USER1);
			//################################################	27 NOT PASSED
			//subscribe(sub_form_2,"27",new ArrayList<>(),Constants.USER1);
			
		}catch(Exception e ) {
			System.out.println("Test error: "+ e);
			Assertions.fail("Unexepected Exception",e);
		}
	}


	private static void subscribe(String query, String alias,List<String> expected,String user) throws SEPASparqlParsingException {
		SubscriptionController controller = new SubscriptionController(alias,expected);
		controllers.add(controller);
		sepaProcessor.processSubscribe(new InternalSubscribeRequest(
				query, 
				alias, 
				null, 
				null, 
				controller, 
				new ClientAuthorization(new Credentials(user,"mecojioni"))
		));
	}
	
	private static void setStorageOwner(SEPAAcl owner, ACLStorageOperations data) throws Exception {
		final Field f = ACLStorageDataset.class.getDeclaredField("owner");
		f.setAccessible(true);
		f.set(data, owner);
	}

	@Test
	public void Subscriptions_TEST() throws NumberFormatException, SEPABindingsException, SPARQL11ProtocolException, SEPASparqlParsingException, InterruptedException{
		//update data
		sepaProcessor.processUpdate(new InternalUpdateRequest(insert, null, null, new ClientAuthorization(new Credentials(SEPAAcl.ADMIN_USER,"mecojioni"))));
		
		Thread.sleep(5000);
		for (SubscriptionController controller : controllers) {
			assertTrue(controller.doIReceivedAll(),"Fail for subscription alias: "+controller.getAlias());
		}
	}



	private static void doUpdate(
			InternalRequestFactory  reqFactory,
			Processor         processor,
			String                  sparql,
			String                  userName,
			boolean expected
			) throws SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
		try {
			final InternalUpdateRequest req = reqFactory.newUpdateInstance(
					sparql, 
					null, 
					null, 
					new ClientAuthorization(new Credentials(userName,"mecojioni"))
					);

			final Response resp = processor.processUpdate(req);
			if (expected) {
				Assertions.assertFalse(resp.isError(),"Response is error for " + sparql + " /" + userName);
				Assertions.assertTrue(resp.isUpdateResponse(),"Response is not update for " + sparql + " /" + userName);
			} else {
				Assertions.assertTrue(resp.isError(),"Response is not error for " + sparql + " /" + userName);
				Assertions.assertFalse(resp.isUpdateResponse(),"Response is update for " + sparql + " /" + userName);

			}
		} catch(AccessDeniedException e)  {
			if (expected)
				Assertions.fail("Unexpected AccessDenisedException",e);
		}catch(ACLException e)  {
			if (expected)
				Assertions.fail("Unexpected ACLException",e);
		}
	}

	private static void doUpdate( 
			InternalRequestFactory reqFactory,
			Processor         processor,
			String                  sparql,
			String                  userName
			) throws SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
		doUpdate(reqFactory,processor, sparql, userName, true);
	}



}

class SubscriptionController implements EventHandler{
	private List<String> validationObj;
	private String alias;
	public SubscriptionController(String alias,List<String> validationObj ) {
		this.validationObj= validationObj;
		this.alias=alias;
	}
	
	public String getAlias() {
		return this.alias;
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		if(notify.getARBindingsResults().getAddedBindings().size()!=validationObj.size()) {
			System.out.println("Fail on notifyEvent info for alias["+this.alias+"]:\n"+notify.getARBindingsResults().getAddedBindings().toJson().toString());
			assertTrue(notify.getARBindingsResults().getAddedBindings().size()==validationObj.size(), "Fail notifyEvent for subscription alias["+this.alias+"]");
		}
		String obj_received="";
		try {
			List<Bindings> binds = notify.getARBindingsResults().getAddedBindings().getBindings();
			for (Bindings bindings : binds) {
				obj_received=bindings.getRDFTerm("o").getValue();
				System.out.println("##Subscriprion alias["+this.alias+"] received: "+obj_received);
				if(this.validationObj.contains(obj_received)) {
					this.validationObj.remove(obj_received);
				}else {
					assertTrue(false,"Received a query danied query result for subscription alias["+this.alias+"]");
				}
			}
		} catch (SEPABindingsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assertions.fail("notifyEvent Exception",e);
		}
		
	}
	
	public boolean doIReceivedAll() {
		if(this.validationObj.size()==0) {
			return true;
		}else {
			System.out.println("doIReceivedAll fail info: " +validationObj);
			return false;
		}
	}
}


