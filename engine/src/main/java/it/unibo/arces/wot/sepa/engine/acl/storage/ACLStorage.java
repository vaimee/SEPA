/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.acl.EngineACLException;
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
    
    
    Map<String, SEPAAcl.UserData>                   loadUsers() throws EngineACLException,ACLStorageException;
    Map<String,Map<String, Set<DatasetACL.aclId>>>  loadGroups() throws EngineACLException,ACLStorageException;
    
    SEPAAcl.UserData                                loadUser(String userName) throws EngineACLException,ACLStorageException;
    Map<String, Set<DatasetACL.aclId>>              loadGroup(String groupName) throws EngineACLException,ACLStorageException;
    
    void removeUser(String user) throws EngineACLException,ACLStorageException;;
    void removeUserPermissions(String user,String graph) throws EngineACLException,ACLStorageException;;
    void removeUserPermission(String user,String graph,DatasetACL.aclId id) throws EngineACLException,ACLStorageException;;
    void addUser(String user) throws EngineACLException,ACLStorageException;;
    void addUserPermission(String user, String graph,DatasetACL.aclId id) throws EngineACLException,ACLStorageException;

    void addUserToGroup(String user, String group) throws EngineACLException,ACLStorageException;
    void removeUserFromGroup(String user, String group) throws EngineACLException,ACLStorageException;
    
    
    void removeGroup(String group) throws EngineACLException,ACLStorageException;;
    void removeGroupPermissions(String group,String graph) throws EngineACLException,ACLStorageException;;
    void removeGroupPermission(String group,String graph,DatasetACL.aclId id) throws EngineACLException,ACLStorageException;;
    void addGroup(String group) throws EngineACLException,ACLStorageException;;
    void addGroupPermission(String group, String graph,DatasetACL.aclId id) throws EngineACLException,ACLStorageException;;
    
    Map<String,Object> getParams();
    Map<String,String> getParamsInfo();
}
