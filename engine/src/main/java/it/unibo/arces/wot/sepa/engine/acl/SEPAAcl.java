/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl;

import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorage;
import it.unibo.arces.wot.sepa.engine.acl.storage.ACLStorageException;
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
    private final ACLStorage                                aclStorage;
    //local caching of ACL
    
    private final Map<String, Map<String,Set<DatasetACL.aclId>>>        cachedACL;
    
    public static SEPAAcl getInstance(ACLStorage storage) throws ACLStorageException{
        if (aclInstance == null)
            aclInstance = new SEPAAcl(storage);
        
        return aclInstance;
    }
    
    private SEPAAcl(ACLStorage storage) throws ACLStorageException{
        aclStorage = storage;
        cachedACL = aclStorage.load();
    }
    @Override
    public boolean checkGrapBase(aclId id, String graphName, String user) {
        return true;
    }

    @Override
    public Map<String,Map<String,Set<aclId>>>  load() throws ACLStorageException {
        return cachedACL;
    }

    @Override
    public void removeUser(String user) throws ACLStorageException {
        cachedACL.remove(user);
        aclStorage.removeUser(user);
    }

    @Override
    public void removeUserPermissions(String user,String graph) throws ACLStorageException {
        final Map<String, Set<DatasetACL.aclId>> acls = cachedACL.get(user);
        if (acls != null) {
            acls.remove(graph);
        }
        
        aclStorage.removeUserPermissions(user, graph);
    }

    @Override
    public void addUser(String user) throws ACLStorageException {
        cachedACL.put(user , new TreeMap<>());
        aclStorage.addUser(user);
    }

    @Override
    public void addUserPermission(String user, String graph, aclId id) throws ACLStorageException {
        Map<String, Set<aclId>> acls;
        if (cachedACL.containsKey(user)) {
            acls = cachedACL.get(user);
        } else {
           acls = new TreeMap<>();
           cachedACL.put(user, acls);
        }
        
        Set<aclId> specAcl;
        if (acls.containsKey(graph)) {
            specAcl = acls.get(graph);
        } else {
            specAcl = new TreeSet<>();
            acls.put(graph, specAcl);
        }
        
        if (specAcl.contains(id) == false ) {
            specAcl.add(id);
        }
        
        
        aclStorage.addUserPermission(user, graph, id);
            
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
    public void removeUserPermission(String user, String graph, aclId id) throws ACLStorageException {
        
        final Map<String, Set<aclId>> acls = cachedACL.get(user);
        
        if (acls != null) {
            final Set<aclId> specAcl = acls.get(graph);
            if (specAcl != null) {
                specAcl.remove(id);
            }
        }
        
        aclStorage.removeUserPermission(user, graph, id);
    }

    
}
