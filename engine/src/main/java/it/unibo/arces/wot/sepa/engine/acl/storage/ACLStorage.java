/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
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
    
    
    Map<String, SEPAAcl.UserData>                   loadUsers() throws ACLException,ACLStorageException;
    Map<String,Map<String, Set<DatasetACL.aclId>>>  loadGroups() throws ACLException,ACLStorageException;
    
    void removeUser(String user) throws ACLException,ACLStorageException;;
    void removeUserPermissions(String user,String graph) throws ACLException,ACLStorageException;;
    void removeUserPermission(String user,String graph,DatasetACL.aclId id) throws ACLException,ACLStorageException;;
    void addUser(String user) throws ACLException,ACLStorageException;;
    void addUserPermission(String user, String graph,DatasetACL.aclId id) throws ACLException,ACLStorageException;
    
    void addUserToGroup(String user, String group) throws ACLException,ACLStorageException;
    void removeUserFromGroup(String user, String group) throws ACLException,ACLStorageException;
    
    
    void removeGroup(String group) throws ACLException,ACLStorageException;;
    void removeGroupPermissions(String group,String graph) throws ACLException,ACLStorageException;;
    void removeGroupPermission(String group,String graph,DatasetACL.aclId id) throws ACLException,ACLStorageException;;
    void addGroup(String group) throws ACLException,ACLStorageException;;
    void addGroupPermission(String group, String graph,DatasetACL.aclId id) throws ACLException,ACLStorageException;;
    
    Map<String,Object> getParams();
    Map<String,String> getParamsInfo();
}
