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
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientAuthorization;
import it.unibo.arces.wot.sepa.commons.security.Credentials;
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
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Assertions;

public class ITACLEndpoints {
    private  final   InternalAclRequestFactory       reqFactory = new InternalAclRequestFactory();
    private  final   ClientAuthorization             dummyAdminAuth = new ClientAuthorization(new Credentials(SEPAAcl.ADMIN_USER,"mecojioni"));
    public ITACLEndpoints() {
        
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
            //load ACL with default test data
            doUpdate(sepaUpdater, initGroupsQuery);
            doUpdate(sepaUpdater, initQuery);
            
            
        }catch(Exception e ) {
            Assertions.fail(e);
        }
    }
    
    
    private void doUpdate( UpdateProcessor sepaUpdater,String sparql) throws SPARQL11ProtocolException, SEPASparqlParsingException, SEPASecurityException, IOException {
        final InternalUpdateRequest req = reqFactory.newUpdateInstance(sparql, null, null, dummyAdminAuth);
        
        final Response r = sepaUpdater.process(req);
        Assertions.assertFalse(r.isError());
        
        
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
