/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import java.util.Map;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageException extends Exception {
    private final ACLStorage.ACLStorageId   id;
    private final Map<String,Object>        params;
    
    public ACLStorage.ACLStorageId getId() {
        return id;
    }
    public Map<String,Object> getParams() {
        return params;
    }
    public ACLStorageException() {
        super();
        id = null;
        params = null;
    }
    //on create 
    public ACLStorageException(ACLStorage.ACLStorageId id, Map<String,Object> params) {
        super();
        this.id = id;
        this.params = params;
    }
    
    public ACLStorageException(String msg, ACLStorage.ACLStorageId id, Map<String,Object> params) {
        super(msg);
        this.id = id;
        this.params = params;
    }
    
    
}
