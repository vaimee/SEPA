/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl;

import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageException;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorage;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageOperations;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public class SEPAAcl extends DatasetACL implements ACLStorage{
    private static SEPAAcl          aclInstance;
    
    //where persistence is archieved
    private final ACLStorageOperations                                aclStorage;

    @Override
    public void addUserToGroup(String user, String group) throws EngineACLException,ACLStorageException {
        if (cachedGroupsACL.containsKey(group) == false) {
            throw new EngineACLException("Group not found " + group);
        }
        
        final SEPAAcl.UserData ud = cachedACL.get(user);
        if (ud == null)
            throw new EngineACLException("User not found " + user);
        
        ud.memberOf.add(group);
        
        aclStorage.addUserToGroup(user, group);

    }

    @Override
    public void removeUserFromGroup(String user, String group) throws EngineACLException,ACLStorageException {
        final SEPAAcl.UserData ud = cachedACL.get(user);
        if (ud == null)
            throw new EngineACLException("User not found ");
        
        ud.memberOf.remove(group);
        
        aclStorage.removeUserFromGroup(user, group);

    }
    //local caching of ACL
    public static class UserData {
        public final Set<String>                            memberOf    = new TreeSet<>();
        public final Map<String,Set<DatasetACL.aclId>>      graphACLs   = new TreeMap<>();
    }
    
    
    private final Map<String, UserData>                             cachedACL;
    private final Map<String,Map<String, Set<DatasetACL.aclId>>>    cachedGroupsACL;
    
    public static SEPAAcl getInstance(ACLStorageOperations storage) throws EngineACLException,ACLStorageException{
        if (aclInstance == null)
            aclInstance = new SEPAAcl(storage);
        
        return aclInstance;
    }
    
    private SEPAAcl(ACLStorageOperations storage) throws EngineACLException,ACLStorageException{
        aclStorage = storage;
        cachedACL = aclStorage.loadUsers();
        cachedGroupsACL = aclStorage.loadGroups();
    }
    @Override
    public boolean checkGrapBase(aclId id, String graphName, String user) {
        if (user.equals(ADMIN_USER))
            return true;
        
        return true;
    }

    @Override
    public Map<String, UserData> loadUsers() throws EngineACLException,ACLStorageException {
        return cachedACL;
    }

    @Override
    public void removeUser(String user) throws EngineACLException,ACLStorageException {
        if (cachedACL.containsKey(user) == true) {
            cachedACL.remove(user);
            aclStorage.removeUser(user);
        } else {
           // throw new ACL
        }
    }

    @Override
    public void removeUserPermissions(String user,String graph) throws EngineACLException,ACLStorageException {
        final UserData acls = cachedACL.get(user);
        if (acls != null && acls.graphACLs != null) {
            acls.graphACLs.remove(graph);
        }
        
        aclStorage.removeUserPermissions(user, graph);
    }

    @Override
    public void addUser(String user) throws EngineACLException,ACLStorageException {
        if (cachedACL.containsKey(user) == true) {
            throw new EngineACLException("User already exists : " + user);
        }
        cachedACL.put(user , new UserData());
        aclStorage.addUser(user);
    }

    @Override
    public void addUserPermission(String user, String graph, aclId id) throws EngineACLException,ACLStorageException {
        UserData ud;
        if (cachedACL.containsKey(user)) {
            ud  = cachedACL.get(user);
        } else {
           ud = new UserData();
           cachedACL.put(user, ud);
        }
        final Map<String,Set<aclId>> acls = ud.graphACLs;
        
        Set<aclId> specAcl;
        if (acls.containsKey(graph)) {
            specAcl = acls.get(graph);
            if (specAcl.contains(id) == false) {
                specAcl.add(id);
                aclStorage.addUserPermission(user, graph, id);
            }
        } else {
            specAcl = new TreeSet<>();
            specAcl.add(id);
            acls.put(graph, specAcl);
            aclStorage.addGraphToUser(user, graph,id);
        }
        
        
        
        
            
    }

    @Override
    public Map<String, Object> getParams() {
        return aclStorage.getParams();
    }

    @Override
    public Map<String, String> getParamsInfo() {
        return aclStorage.getParamsInfo();
    } 

    @Override
    public void removeUserPermission(String user, String graph, aclId id) throws EngineACLException,ACLStorageException {
        
        final UserData ud = cachedACL.get(user);;
        if (ud != null){ 
            final Map<String, Set<aclId>> acls = ud.graphACLs;

            if (acls != null) {
                final Set<aclId> specAcl = acls.get(graph);
                if (specAcl != null) {
                    specAcl.remove(id);
                }
            }
        }

        aclStorage.removeUserPermission(user, graph, id);

    }

    @Override
    public Map<String,Map<String, Set<DatasetACL.aclId>>> loadGroups() throws EngineACLException,ACLStorageException {
        return cachedGroupsACL;
    }

    @Override
    public void removeGroup(String group) throws EngineACLException,ACLStorageException {
        cachedGroupsACL.remove(group);
        aclStorage.removeGroup(group);
        
        //next, remove all reference of this group from all users
        for(final Map.Entry<String, UserData> e : cachedACL.entrySet()) {
            final UserData ud = e.getValue();
            if (ud.memberOf != null && ud.memberOf.contains(group)) {
                    ud.memberOf.remove(group);
                    aclStorage.removeUserFromGroup(e.getKey(), group);
            }
        }
    }

    @Override
    public void removeGroupPermissions(String group,String graph) throws EngineACLException,ACLStorageException {
        final Map<String, Set<DatasetACL.aclId>> acls = cachedGroupsACL.get(group);
        if (acls != null) {
            acls.remove(graph);
        }
        
        aclStorage.removeGroupPermissions(group, graph);
    }

    @Override
    public void addGroup(String group) throws EngineACLException,ACLStorageException {
        if (cachedGroupsACL.containsKey(group))
            throw new EngineACLException("Group already exists : " + group);
        
        cachedGroupsACL.put(group , new TreeMap<>());
        aclStorage.addGroup(group);
    }

    @Override
    public void addGroupPermission(String group, String graph, aclId id) throws EngineACLException,ACLStorageException {
        Map<String, Set<aclId>> acls;
        if (cachedGroupsACL.containsKey(group)) {
            acls = cachedGroupsACL.get(group);
        } else {
           acls = new TreeMap<>();
           cachedGroupsACL.put(group, acls);
        }
        
        Set<aclId> specAcl;
        if (acls.containsKey(graph)) {
            specAcl = acls.get(graph);
            if (specAcl.contains(id) == false ) {
                specAcl.add(id);
                aclStorage.addGroupPermission(group, graph, id);
            }
        } else {
            specAcl = new TreeSet<>();
            specAcl.add(id);
            acls.put(graph, specAcl);
            aclStorage.addGraphToGroup(group, graph,id);
        }
        
            
    }
    
    @Override
    public void removeGroupPermission(String group, String graph, aclId id) throws EngineACLException,ACLStorageException {
        
        final Map<String, Set<aclId>> acls = cachedGroupsACL.get(group);
        if (acls == null) {
            throw new EngineACLException("Group does not exists : " + group);
        }

        final Set<aclId> specAcl = acls.get(graph);
        if (specAcl != null) {
            specAcl.remove(id);
        }
        
        aclStorage.removeGroupPermission(group, graph, id);
    }
    
}
