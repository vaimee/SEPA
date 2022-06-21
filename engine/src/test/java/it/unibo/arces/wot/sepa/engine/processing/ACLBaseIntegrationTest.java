/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing;

/**
 *
 * @author Lorenzo
 */

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASparqlParsingException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.security.Credentials;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageDataset;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageException;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageOperations;
import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initGroupsQuery;
import static it.unibo.arces.wot.sepa.engine.acl.storage.Constants.initQuery;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import org.junit.jupiter.api.Test;
import it.unibo.arces.wot.sepa.engine.core.Engine;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.endpoint.ACLTools;
import it.unibo.arces.wot.sepa.engine.protocol.sparql11.SPARQL11ProtocolException;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalAclRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalStdRequestFactory;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import org.apache.jena.acl.ACLException;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.shared.AccessDeniedException;
import org.junit.jupiter.api.Assertions;

public class ACLBaseIntegrationTest {
    private  final   InternalAclRequestFactory       aclReqFactory = new InternalAclRequestFactory();
    private  final   InternalStdRequestFactory       stdReqFactory = new InternalStdRequestFactory();
    //direct copy & paste from ACLStorageDataset.java
    private static final  String    SEPACL_NS_PFIX              =          "<http://acl.sepa.com/>";
    private static final  String    SEPACL_NS_GRP_PFIX          =          "<http://groups.acl.sepa.com/>";
    private static final  String    SEPACL_GRAPH_NAME           =          "sepaACL:acl";
    private static final  String    SEPACL_GRAPH_GROUP_NAME     =          "sepaACL:aclGroups";
    //should succeed for monger and fail for gonger
    
    private final String  spqlInsertData = 
    "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
    "INSERT DATA  {  "                                          + System.lineSeparator() + 
    "	GRAPH mp:XXGRAPH { "                                    + System.lineSeparator() + 
    "		<http://s1> <http://p1> <http://o1> ."          + System.lineSeparator() + 
    "		<http://s2> <http://p2> <http://o3> ."          + System.lineSeparator() + 
    "		<http://s2> <http://p3> <http://o3> ."          + System.lineSeparator() + 
    "		<http://s3> <http://p3> <http://o3> ."          + System.lineSeparator() + 
    "		<http://s3> <http://p1> <http://o3> ."          + System.lineSeparator() + 
    "	}}" ;
	            
    private final String selectQuery1 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH mp:graph1  {?s ?p ?o}}";
        
    private final String selectQuery2= 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH mp:graph2  {?s ?p ?o}}";
    
    private final String selectQuery3 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH  mp:graph3  {?s ?p ?o}}";

    private final String selectQuery4 = 
            "PREFIX mp: <http://mysparql.com/> "                        + System.lineSeparator() + 
            "SELECT ?s ?p ?o WHERE {GRAPH  mp:graph4  {?s ?p ?o}}";
    
    public ACLBaseIntegrationTest() {
        
    }
    private void checkInsertData(
        InternalRequestFactory      reqFactory,
        UpdateProcessor             sepaUpdater, 
        String                      user,
        String                      graph,
        boolean                     expect
    )throws Exception {
        final String finalQuery  = spqlInsertData.replaceAll("XXGRAPH", graph);
        doUpdate(reqFactory,sepaUpdater, finalQuery, user,expect);
        
        
    }
    private void checkUser2(UpdateProcessor up,QueryProcessor qp) throws Exception { 
        //NOTE: requires checkUser1 to be called before
        final String userName = "gonger";
        
        
        checkInsertData(stdReqFactory,up, userName, "graph1",false);
        checkInsertData(stdReqFactory,up, userName, "graph2",true);
        //now query on graph 2
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery2,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
        //now query on graph 1
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery1,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 0;
                    }
                }
        );
        

        //check that group access works
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery4,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
     
      //removes user2 from group2 
        removeUserFromGroup(up, userName, "group2");
        
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery4,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 0;
                    }
                }
        );
        
      //adds user2 to group1
        addUserToGroup(up, userName, "group1");
        //and check reads
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery1,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery3,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
        
    }
    private void checkUser1(UpdateProcessor up,QueryProcessor qp) throws Exception {
        //monger can : update/query graph2
        //do insert
        final String userName = "monger";
        
        
        checkInsertData(stdReqFactory,up, userName, "graph1",false);
        checkInsertData(stdReqFactory,up, userName, "graph2",false);
        checkInsertData(stdReqFactory,up, DatasetACL.ADMIN_USER, "graph1",true);
        //now query on graph 1
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery1,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
        
        //insert data in graph3 as admin 
        checkInsertData(stdReqFactory,up, DatasetACL.ADMIN_USER, "graph3",true);
        //check that group access works
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery3,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 5;
                    }
                }
        );
     
        //insert data in graph4 as admin
        checkInsertData(stdReqFactory,up, DatasetACL.ADMIN_USER, "graph4",true);
        
        doQuery(
                stdReqFactory,
                qp, 
                selectQuery4,
                userName,
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        return resp.getBindingsResults().getBindings().size() == 0;
                    }
                }
        );
        
        
    }
    private void checkUserList(QueryProcessor qp) throws Exception {
        final String selectQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "SELECT * WHERE { GRAPH " + SEPACL_GRAPH_NAME + " { ?user sepaACL:userName ?value }}";
        doQuery(
                aclReqFactory,
                qp, 
                selectQuery, 
                SEPAAcl.ADMIN_USER, 
                new QueryResponseValidator() {
                    @Override
                    public boolean validate(QueryResponse resp) {
                        final BindingsResults br = resp.getBindingsResults();

                        for (final Bindings bs : br.getBindings()) {
                            final String usrName = bs.getValue("value");
                            switch(usrName) {
                                case "monger":
                                case "gonger":
                                    continue;
                                default:
                                    return false;
                            }

                        }
                        
                        return true;
                    }
                }
            );
    }
    private void checkGroupList(QueryProcessor qp ) throws Exception {
        final String selectQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "SELECT * WHERE { GRAPH " + SEPACL_GRAPH_GROUP_NAME + " { ?group sepaACL:groupName ?value }}";
            doQuery(
                    aclReqFactory,
                    qp, 
                    selectQuery, 
                    SEPAAcl.ADMIN_USER, 
                    new QueryResponseValidator() {
                        @Override
                            public boolean validate(QueryResponse resp) {
                                final BindingsResults br = resp.getBindingsResults();
                                Assertions.assertTrue(br.getBindings().size() > 0 ,"Group count must be > 0 ");
                                for (final Bindings bs : br.getBindings()) {
                                    final String gprName = bs.getValue("value");
                                    switch(gprName) {
                                        case "group1":
                                        case "group2":
                                            continue;
                                        default:
                                            return false;
                                    }
                                }
                                 
                                return true;
                            }
                        });
        
        
    }
    @Test 
    public void testACLEndpoints() {
        try {
            final SPARQL11Properties sepaEndpointProps = new SPARQL11Properties(Engine.defaultEndpointJpar);
            EngineBeans.setEngineProperties(EngineProperties.load("engine.jpar"));
            sepaEndpointProps.setProtocolScheme(SPARQL11Properties.ProtocolScheme.SJenarAPI);
            //adjust properties
            adjustEngineProperties();
            
            //creates ACL objects
            final ACLStorageOperations      aclStorage = ACLTools.makeACLStorage();
            final SEPAAcl                   aclData = SEPAAcl.getInstance(aclStorage);
            setStorageOwner(aclData, aclStorage);
            
            //
            final UpdateProcessor sepaUpdater = new UpdateProcessor(sepaEndpointProps);
            final QueryProcessor sepaQuerier = new QueryProcessor(sepaEndpointProps);
            //load ACL with default test data
            doUpdate(aclReqFactory, sepaUpdater, initGroupsQuery,SEPAAcl.ADMIN_USER);
            doUpdate(aclReqFactory,sepaUpdater, initQuery,SEPAAcl.ADMIN_USER);
            //first. do some query on ACL to check for loaded data
            checkGroupList(sepaQuerier);
            checkUserList(sepaQuerier);
            
            //now, starts graph tests
            checkUser1(sepaUpdater, sepaQuerier);
            checkUser2(sepaUpdater, sepaQuerier);
        }catch(Exception e ) {
        	System.out.println("Test error: "+ e);
            Assertions.fail("Unexepected Exception",e);
        }
    }
    
    private void doQuery(
        InternalRequestFactory  reqFactory, 
        QueryProcessor          sepaQuerier, 
        String                  sparql, 
        String                  userName,
        QueryResponseValidator  rv 
    ) throws SEPASparqlParsingException, SEPASecurityException, IOException {
        final InternalQueryRequest  req = reqFactory.newQueryInstance(
            sparql, 
            null, 
            null, 
            new ClientAuthorization(new Credentials(userName,"mecojioni"))
        );
        
        final Response resp = sepaQuerier.process(req);
        
        Assertions.assertFalse(resp.isError(),"Response is error for " + sparql + " /" + userName);
        Assertions.assertTrue(resp.isQueryResponse(),"Response is not query " + sparql + " /" + userName);
        
        
        if (rv != null) {
            final QueryResponse qresp = (QueryResponse) resp;
            Assertions.assertTrue(rv.validate(qresp),"Response validation failed for " + sparql + " /" + userName);
        }
        
    }
    private void doUpdate(
        InternalRequestFactory  reqFactory,
        UpdateProcessor         sepaUpdater,
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

            final Response resp = sepaUpdater.process(req);
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
    
    private void doUpdate( 
        InternalRequestFactory reqFactory,
        UpdateProcessor         sepaUpdater,
        String                  sparql,
        String                  userName
    ) throws SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
        doUpdate(reqFactory,sepaUpdater, sparql, userName, true);
    }
    private void setStorageOwner(SEPAAcl owner, ACLStorageOperations data) throws Exception {
        final Field f = ACLStorageDataset.class.getDeclaredField("owner");
        f.setAccessible(true);
        f.set(data, owner);
    }
    
    private void adjustEngineProperties() {
        EngineBeans.setAclEnabled(true);
        EngineBeans.setLUTTEnabled(false);
    }
    
    
    private void addUserToGroup(UpdateProcessor up,String user, String group) throws ACLStorageException, SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
        final String baseQuery  =
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                             + System.lineSeparator()    +
                "INSERT DATA { GRAPH " + SEPACL_GRAPH_NAME          +"  { "     + System.lineSeparator()    +  
                "   sepaACL:$$USERNAME sepaACL:memberOf     \"$$GROUPNAME\" "   + System.lineSeparator()    +  
                "}}";
        
        final String finalQuery = baseQuery .replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$USERNAME", user);
        
        doUpdate(aclReqFactory, up, finalQuery, DatasetACL.ADMIN_USER);
        

    }

    
    private void removeUserFromGroup(UpdateProcessor up,String user, String group) throws ACLStorageException, SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
       final String baseQuery  =
                "PREFIX sepaACL: " + SEPACL_NS_PFIX                             + System.lineSeparator()    +
                "DELETE WHERE { GRAPH " + SEPACL_GRAPH_NAME          +"  { "    + System.lineSeparator()    +  
                "   sepaACL:$$USERNAME sepaACL:memberOf     \"$$GROUPNAME\" "   + System.lineSeparator()    +  
                "}}";
        
        final String finalQuery = baseQuery .replaceAll("\\$\\$GROUPNAME", group)
                                            .replaceAll("\\$\\$USERNAME", user);
        
        doUpdate(aclReqFactory, up, finalQuery, DatasetACL.ADMIN_USER);
        

        
    }
    
}
