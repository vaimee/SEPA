/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing.endpoint;

import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorage;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorage.ACLStorageId;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageDataset;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageFactory;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageOperations;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.GlobalSystemProperties;
import java.util.Map;
import java.util.TreeMap;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public class ACLTools {
    public static ACLStorage.ACLStorageId decodeId(String mode) {
        ACLStorage.ACLStorageId ret = null;
        switch(mode.trim().toLowerCase()) {
            case EngineProperties.ACL_TYPE_DS:
                ret = ACLStorageId.asiDataset;
                break;
            case EngineProperties.ACL_TYPE_JSON:
                ret = ACLStorageId.aiJSon;
                break;
        }
        
        return ret;

    }
    
    public static Map<String,Object> makeStorageParamMap(ACLStorage.ACLStorageId  id ) {
        final Map<String,Object>  paramMap = new TreeMap<>();
        
        switch(id) {
            case aiJSon: {
                break;
            }
            case asiDataset: {
                switch(EngineBeans.getAclMode().trim().toUpperCase()) {
                    case EngineProperties.DS_MODE_MEM:
                        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_NONE);
                        break;
                    case EngineProperties.DS_MODE_TDB2:
                        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB2);
                        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,EngineBeans.getAclPath());
                        break;
                    case EngineProperties.DS_MODE_TDB1:
                        paramMap.put(ACLStorageDataset.PARAM_DATASETPERSISTENCY, ACLStorageDataset.PARAM_DATASETPERSISTENCY_VALUE_TDB1);
                        paramMap.put(ACLStorageDataset.PARAM_DATASETPATH,EngineBeans.getAclPath());
                        break;
                        
                }
            }
        }
        return paramMap;
        
    }
    
    public static  ACLStorageOperations   makeACLStorage() {
        final ACLStorage.ACLStorageId id = decodeId(EngineBeans.getAclType());
        final Map<String,Object>      paramMap = makeStorageParamMap(id);
        
        final ACLStorageOperations  ret = ACLStorageFactory.newInstance(id, paramMap);
        
        
        //if test, populate with default values FOR TESTING ONLY
        if (GlobalSystemProperties.checkIfACLIntegrationTest()) {
            ret.addUser("user1");
            ret.addGraphToUser("monger", "http://mysparql.com/gtaph1", DatasetACL.aclId.aiCreate);
            ret.addGraphToUser("monger", "http://mysparql.com/gtaph1", DatasetACL.aclId.aiQuery);
            ret.addGraphToUser("monger", "http://mysparql.com/gtaph1", DatasetACL.aclId.aiUpdate);
        }
        //end of integration test code
        return ret;
    }
}
