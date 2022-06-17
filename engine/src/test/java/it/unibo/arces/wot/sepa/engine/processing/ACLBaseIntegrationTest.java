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
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Assertions;

public class ACLBaseIntegrationTest {
    private  final   InternalAclRequestFactory       reqFactory = new InternalAclRequestFactory();
    //direct copy & paste from ACLStorageDataset.java
    private static final  String    SEPACL_NS_PFIX              =          "<http://acl.sepa.com/>";
    private static final  String    SEPACL_NS_GRP_PFIX          =          "<http://groups.acl.sepa.com/>";
    private static final  String    SEPACL_GRAPH_NAME           =          "sepaACL:acl";
    private static final  String    SEPACL_GRAPH_GROUP_NAME     =          "sepaACL:aclGroups";
    
    
    public ACLBaseIntegrationTest() {
        
    }
    private boolean checkInsertData(String user,String graph)throws Exception {
        return true;
    }
    private void checkUser1(QueryProcessor qp) throws Exception {
        //monger can : update/query graph2
        //do insert
        final String userName = "monger";
        
        boolean f = checkInsertData(userName, "mp:graph1");
        
        
    }
    private void checkUserList(QueryProcessor qp) throws Exception {
        final String selectQuery = 
                "PREFIX sepaACL: " + SEPACL_NS_PFIX + System.lineSeparator()                    +
                "SELECT * WHERE { GRAPH " + SEPACL_GRAPH_NAME + " { ?user sepaACL:userName ?value }}";
        doQuery(
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
                    qp, 
                    selectQuery, 
                    SEPAAcl.ADMIN_USER, 
                    new QueryResponseValidator() {
                        @Override
                            public boolean validate(QueryResponse resp) {
                                final BindingsResults br = resp.getBindingsResults();
                                
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
            final EngineProperties   sepaEngineProps = EngineProperties.getIstance();
            
            //adjust properties
            adjustEngineProperties(sepaEngineProps);
            //set them to Bean
            EngineBeans.setEngineProperties(sepaEngineProps);
            
            //creates ACL objects
            final ACLStorageOperations      aclStorage = ACLTools.makeACLStorage();
            final SEPAAcl                   aclData = SEPAAcl.getInstance(aclStorage);
            setStorageOwner(aclData, aclStorage);
            
            //
            final UpdateProcessor sepaUpdater = new UpdateProcessor(sepaEndpointProps);
            final QueryProcessor sepaQuerier = new QueryProcessor(sepaEndpointProps);
            //load ACL with default test data
            doUpdate(sepaUpdater, initGroupsQuery,SEPAAcl.ADMIN_USER);
            doUpdate(sepaUpdater, initQuery,SEPAAcl.ADMIN_USER);
            //first. do some query on ACL to check for loaded data
            checkGroupList(sepaQuerier);
            checkUserList(sepaQuerier);
            
            //now, starts graph tests
            
        }catch(Exception e ) {
            Assertions.fail(e);
        }
    }
    
    private void doQuery(QueryProcessor sepaQuerier, String sparql, String userName,QueryResponseValidator rv ) throws SEPASparqlParsingException, SEPASecurityException, IOException {
        final InternalQueryRequest  req = reqFactory.newQueryInstance(
            sparql, 
            null, 
            null, 
            new ClientAuthorization(new Credentials(userName,"mecojioni"))
        );
        
        final Response resp = sepaQuerier.process(req);
        
        Assertions.assertFalse(resp.isError());
        Assertions.assertTrue(resp.isQueryResponse());
        
        
        if (rv != null) {
            final QueryResponse qresp = (QueryResponse) resp;
            Assertions.assertTrue(rv.validate(qresp));
        }
        
    }
    private void doUpdate( UpdateProcessor sepaUpdater,String sparql,String userName) throws SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
        final InternalUpdateRequest req = reqFactory.newUpdateInstance(
                sparql, 
                null, 
                null, 
                new ClientAuthorization(new Credentials(userName,"mecojioni"))
        );
        
        final Response resp = sepaUpdater.process(req);
        Assertions.assertFalse(resp.isError());
        Assertions.assertTrue(resp.isUpdateResponse());
        
        
    }
    private void setStorageOwner(SEPAAcl owner, ACLStorageOperations data) throws Exception {
        final Field f = ACLStorageDataset.class.getDeclaredField("owner");
        f.setAccessible(true);
        f.set(data, owner);
    }
    private void adjustEngineProperties(EngineProperties p) throws Exception  {
        final Field paramsField = EngineProperties.class.getDeclaredField("parameters");
        
        paramsField.setAccessible(true);
        
        final Object paramsData = paramsField.get(p);
        
        //now go deep in ACL
        
        final Field aclField = paramsData.getClass().getField("acl");
        aclField.setAccessible(true);
        final Object aclData = aclField.get(paramsData);
        
        final Field aclEnabledField = aclData.getClass().getField("enabled");
        aclEnabledField.setAccessible(true);
        aclEnabledField.set(aclData, true);
        
        
    }
}
