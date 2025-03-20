/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vaimee.sepa.engine.processing;

import com.vaimee.sepa.engine.dependability.acl.SEPAAcl;
import com.vaimee.sepa.engine.bean.SEPABeans;

import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.apache.jena.acl.DatasetACL.aclId.aiClear;
import static org.apache.jena.acl.DatasetACL.aclId.aiCreate;
import static org.apache.jena.acl.DatasetACL.aclId.aiDeleteData;
import static org.apache.jena.acl.DatasetACL.aclId.aiDrop;
import static org.apache.jena.acl.DatasetACL.aclId.aiInsertData;
import static org.apache.jena.acl.DatasetACL.aclId.aiQuery;
import static org.apache.jena.acl.DatasetACL.aclId.aiUpdate;

/**
 *
 * @author Lorenzo
 */
public class SEPAAclProcessor implements SEPAAclProcessorMBean {
    private static final Map<DatasetACL.aclId, String>      encodeMap = new TreeMap<>();
    private static final Map<String,DatasetACL.aclId>       decodeMap = new TreeMap<>();
    
    private static void addEncodeDecodeElement(DatasetACL.aclId id, String name) {
        encodeMap .put(id, name);
        decodeMap.put(name, id);
    }
    
    private static DatasetACL.aclId decodeACLId(String u) {
        DatasetACL.aclId ret = null;
        if (decodeMap.containsKey(u.trim().toLowerCase())) {
            ret = decodeMap.get(u);
        }
        return ret;
    }
    
    private static String encodeACLId(DatasetACL.aclId id) {
        String ret = null;
        if (encodeMap.containsKey(id)) {
            ret = encodeMap.get(id);
        }
        
        return ret;
    }
    static {
        addEncodeDecodeElement(aiDrop,"drop");
        addEncodeDecodeElement(aiClear,"clear");
        addEncodeDecodeElement(aiCreate,"create");
        addEncodeDecodeElement(aiInsertData,"insertdata");
        addEncodeDecodeElement(aiDeleteData,"deletedata");
        addEncodeDecodeElement(aiUpdate,"update");
        addEncodeDecodeElement(aiQuery,"query");
    
    }
    private static Map<String, Set<String>> marshallAccessMap(Map<String,Set<DatasetACL.aclId>> m) {
        final Map<String, Set<String>> ret = new TreeMap<>();
        for(final Map.Entry<String,Set<DatasetACL.aclId>> e : m.entrySet()) {
            final String key = e.getKey();
            final Set<String>  value = new TreeSet<>();
            for(final DatasetACL.aclId id : e.getValue()) {
                final String n = encodeACLId(id);
                value.add(n);
            }
            
            ret.put(key, value);
        }
        
        return ret;
    }
    public SEPAAclProcessor() {
        SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
    }
    @Override
    public String reloadUsers() {
        String ret ="N/A";
        try {
            final SEPAAcl acl = SEPAAcl.getInstance();
            if (acl != null) {
                acl.loadUsers();
                ret = "OK";

            }
        } catch(Exception e ) {
            ret = "ERROR: " + e.getMessage();
        }
        return ret;
    }

    @Override
    public String  reloadGroups() {
        String ret ="N/A";
        try {
            final SEPAAcl acl = SEPAAcl.getInstance();
            if (acl != null) {
                acl.loadGroups();
                ret = "OK";

            }
        } catch(Exception e ) {
            ret = "ERROR: " + e.getMessage();
        }
        return ret;

    }

    @Override
    public Map<String,List<Object>> listUsers() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, SEPAAcl.UserData> tmp = (acl != null ? acl.listUsers() : null);
        
        final Map<String,List<Object>> ret = tmp == null ? null : new TreeMap<>();
        
        if (ret != null) {
            for(final Map.Entry<String, SEPAAcl.UserData> e : tmp.entrySet()) {
                final List<Object> z = new ArrayList();
                z.add(marshallAccessMap(e.getValue().graphACLs));
                z.add(e.getValue().memberOf);
                ret.put(e.getKey(), z);
            }
        }
        return ret;
           
    }

    @Override
    public Map<String, Map<String, Set<String>>> listGroups() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, Map<String, Set<DatasetACL.aclId>>> tmp = (acl != null ? acl.listGroups() : null);
        
        final Map<String, Map<String, Set<String>>> ret  = (tmp == null ? null : new TreeMap<>());
        if (ret != null) {
            for (final Map.Entry<String, Map<String, Set<DatasetACL.aclId>>> e : tmp.entrySet()) {
                final String key = e.getKey();
                final  Map<String, Set<String>> value = marshallAccessMap(e.getValue());
                
                ret.put(key, value);
            }
        }
        return ret;

    }

    @Override
    public List<Object> viewUser(String name) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final SEPAAcl.UserData tmp = (acl != null ? acl.viewUser(name): null);
        final List<Object> ret = (tmp == null ? null : new ArrayList<>());
        if (ret != null) {
                ret.add(marshallAccessMap(tmp.graphACLs));
                ret.add(tmp.memberOf);

        }
        return ret;

    }

    @Override
    public Map<String, Set<String>> viewGroup(String name) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, Set<DatasetACL.aclId>> tmp = (acl != null ? acl.viewGroup(name): null);
        final Map<String, Set<String>>  ret = (tmp == null ? null : marshallAccessMap(tmp));
        return ret;
 
    }

    @Override
    public String  reloadUser(String userName) {
        String ret ="N/A";
        try {
            final SEPAAcl acl = SEPAAcl.getInstance();
            if (acl != null) {
                acl.loadUser(userName);
                ret = "OK";

            }
        } catch(Exception e ) {
            ret = "ERROR: " + e.getMessage();
        }
        return ret;


    }

    @Override
    public String  reloadGroup(String groupName) {
        String ret ="N/A";
        try {

            final SEPAAcl acl = SEPAAcl.getInstance();
            if (acl != null) {
                acl.loadGroup(groupName);
                ret = "OK";

            }
        } catch(Exception e ) {
            ret = "ERROR: " + e.getMessage();
        }
        return ret;


    }

    @Override
    public String  removeUser(String user) {
        return methodInvoker("removeUser", new Object[] {user});
    }

    @Override
    public String  removeUserPermissions(String user, String graph) {
        return methodInvoker("removeUserPermissions", new Object[] {user,graph});
    }

    @Override
    public String  removeUserPermission(String user, String graph, String id) {
        return methodInvoker("removeUserPermission", new Object[] {user,graph,decodeACLId(id)});
    }

    @Override
    public String  addUser(String user) {
        return methodInvoker("addUser", new Object[] {user});
    }

    @Override
    public String  addUserPermission(String user, String graph, String id) {
        return methodInvoker("addUserPermission", new Object[] {user,graph,decodeACLId(id)});
    }

    @Override
    public String  addUserToGroup(String user, String group) {
        return methodInvoker("addUserToGroup", new Object[] {user,group});
    }

    @Override
    public String  removeUserFromGroup(String user, String group) {
        return methodInvoker("removeUserFromGroup", new Object[] {user,group});
    }

    @Override
    public String  removeGroup(String group) {
        return methodInvoker("removeGroup", new Object[] {group});
    }

    @Override
    public String  removeGroupPermissions(String group, String graph) {
        return methodInvoker("removeGroupPermissions", new Object[] {group,graph});
    }

    @Override
    public String  removeGroupPermission(String group, String graph, String id) {
        return methodInvoker("removeGroupPermission", new Object[] {group,graph,decodeACLId(id)});
    }

    @Override
    public String  addGroup(String group) {
        return methodInvoker("addGroup", new Object[] {group});
    }

    @Override
    public String  addGroupPermission(String group, String graph, String id) {
        return methodInvoker("addGroupPermission", new Object[] {group,graph,decodeACLId(id)});
    }

    @Override
    public Map<String, Object> getParams() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, Object> ret = (acl != null ? acl.getParams() : null);
        return ret;
    }

    @Override
    public Map<String, String> getParamsInfo() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, String> ret = (acl != null ? acl.getParamsInfo() : null);
        return ret;
    }
    
    private String methodInvoker(String name, Object args[]) {
        String ret = "N/A";
        
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Class cargs[] = new Class[args.length];
        for(int i = 0 ; i < args.length;++i) {
            cargs[i] = args[i].getClass();
        }
        
        if (acl != null) {
            try {
                final Class c = SEPAAcl.class;
                final Method m = c.getDeclaredMethod(name, cargs);
                if (m != null) {
                    m.invoke(acl, args);
                    ret = "OK";
                }
            } catch(Exception e ) {
                System.err.println(e);
                ret = "ERROR: " + e.getMessage();
            }
        }
        
        return ret;
    }
    
    
    
    public static void main(String[] args ) {
        final Object a[]  =  { "parama1",12};
        for(final Object o : a) {
            System.out.println(o.getClass().getName());
    }
    }
}
