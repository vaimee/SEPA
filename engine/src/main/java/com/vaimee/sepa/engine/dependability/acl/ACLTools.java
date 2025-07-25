/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.dependability.acl;

import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorage;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorage.ACLStorageId;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageDataset;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageFactory;
import com.vaimee.sepa.engine.dependability.acl.storage.ACLStorageOperations;
import com.vaimee.sepa.engine.bean.EngineBeans;
import com.vaimee.sepa.engine.core.EngineProperties;
import java.util.Map;
import java.util.TreeMap;

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
        return ret;
    }
}
