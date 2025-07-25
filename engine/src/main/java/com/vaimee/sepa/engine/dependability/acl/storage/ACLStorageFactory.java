/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.dependability.acl.storage;

import java.util.Map;
/**
 *
 * @author Lorenzo
 */
public abstract class ACLStorageFactory {
    private static  ACLStorageOperations  aclStorageInstance = null;
    public static ACLStorageOperations newInstance(ACLStorage.ACLStorageId id,Map<String,Object> params) throws ACLStorageException {
        if (aclStorageInstance == null) {
            switch(id) {
                case asiDataset:
                    aclStorageInstance = new ACLStorageDataset(params);
                    break;
                case aiJSon:
                    aclStorageInstance = new ACLStorageJSon(params);
                    break;
                case asiSolid:
                    break;
            }
        }
        return aclStorageInstance;
    }
}
