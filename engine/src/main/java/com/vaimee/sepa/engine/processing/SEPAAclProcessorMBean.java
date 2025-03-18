/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public interface SEPAAclProcessorMBean {
    String reloadUsers();
    String reloadGroups();
    String reloadUser(String userName);
    String reloadGroup(String groupName);
    Map<String,List<Object>> listUsers();
    Map<String,Map<String,Set<String>>> listGroups();
    List<Object> viewUser(String name);
    Map<String,Set<String>> viewGroup(String name);
    
    
    String removeUser(String user);
    String removeUserPermissions(String user,String graph);
    String removeUserPermission(String user,String graph,String id);
    String addUser(String user);
    String addUserPermission(String user, String graph,String id);

    String addUserToGroup(String user, String group) ;
    String removeUserFromGroup(String user, String group) ;
    
    
    String removeGroup(String group) ;
    String removeGroupPermissions(String group,String graph);
    String removeGroupPermission(String group,String graph,String id);
    String addGroup(String group);
    String addGroupPermission(String group, String graph,String id) ;
    
    Map<String,Object> getParams();
    Map<String,String> getParamsInfo();    
}
