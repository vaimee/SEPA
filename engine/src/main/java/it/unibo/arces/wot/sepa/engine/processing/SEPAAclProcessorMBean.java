/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl.UserData;
import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public interface SEPAAclProcessorMBean {
    void reloadUsers();
    void reloadGroups();
    void reloadUser(String userName);
    void reloadGroup(String groupName);
    Map<String,UserData> listUsers();
    Map<String,Map<String,Set<DatasetACL.aclId>>> listGroups();
    UserData viewUser(String name);
    Map<String,Set<DatasetACL.aclId>> viewGroup(String name);
    
    
    void removeUser(String user);
    void removeUserPermissions(String user,String graph);
    void removeUserPermission(String user,String graph,DatasetACL.aclId id);
    void addUser(String user);
    void addUserPermission(String user, String graph,DatasetACL.aclId id);

    void addUserToGroup(String user, String group) ;
    void removeUserFromGroup(String user, String group) ;
    
    
    void removeGroup(String group) ;
    void removeGroupPermissions(String group,String graph);
    void removeGroupPermission(String group,String graph,DatasetACL.aclId id);
    void addGroup(String group);
    void addGroupPermission(String group, String graph,DatasetACL.aclId id) ;
    
    Map<String,Object> getParams();
    Map<String,String> getParamsInfo();    
}
