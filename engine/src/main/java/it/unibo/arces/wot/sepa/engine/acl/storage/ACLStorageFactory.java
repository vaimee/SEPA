/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import java.util.Map;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public abstract class ACLStorageFactory {
    
    public static ACLStorage newInstance(ACLStorage.ACLStorageId id,Map<String,Object> params) throws ACLStorageException {
        ACLStorage  ret = null;
        switch(id) {
            case asiDataset:
                ret = new ACLStorageDataset(params);
                break;
            case aiJSon:
                break;
            case asiSolid:
                break;
        }
        return ret;
    }
}
