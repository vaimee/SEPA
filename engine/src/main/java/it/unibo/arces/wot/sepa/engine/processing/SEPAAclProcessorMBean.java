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
    public void reloadUsers();
    public void reloadGroups();
    public void reloadUser(String userName);
    public void reloadGroup(String groupName);
    public Map<String,UserData> listUsers();
    public Map<String,Map<String,Set<DatasetACL.aclId>>> listGroups();
    public UserData viewUser(String name);
    public Map<String,Set<DatasetACL.aclId>> viewGroup(String name);
    
}
