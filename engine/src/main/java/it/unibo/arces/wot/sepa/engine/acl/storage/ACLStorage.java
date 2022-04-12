/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;
import org.apache.jena.acl.DatasetACL.aclId;

/**
 *
 * @author Lorenzo
 */
public interface ACLStorage {
    public enum ACLStorageId {
        asiDataset,
        asiSolid,
        aiJSon
    }
    
    
    Map<String,Map<String,Set<aclId>>>  load() throws ACLStorageException;
    void removeUser(String user) throws ACLStorageException;;
    void removeUserPermissions(String user,String graph) throws ACLStorageException;;
    void removeUserPermission(String user,String graph,DatasetACL.aclId id) throws ACLStorageException;;
    void addUser(String user) throws ACLStorageException;;
    void addUserPermission(String user, String graph,DatasetACL.aclId id) throws ACLStorageException;;
    
    Map<String,Object> getParams();
    Map<String,String> getParamsInfo();
}
