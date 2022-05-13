/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;
import java.lang.reflect.Method;
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
public class SEPAAclProcessor implements SEPAAclProcessorMBean{
    
    private static DatasetACL.aclId decodeACLId(String u) {
        DatasetACL.aclId ret = null;
        switch(u.trim().toLowerCase()) {
            case "drop":
                ret = aiDrop;
                break;
            case "clear":
                ret = aiClear;
                break;
            case "create":
                ret = aiCreate;
                break;
            case "insertdata":
                ret = aiInsertData;
                break;
            case "deletedata":
                ret = aiDeleteData;
                break;
            case "update":
                ret = aiUpdate;
                break;
            case "query":
                ret = aiQuery ;
                break;
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
    public Map<String, SEPAAcl.UserData> listUsers() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, SEPAAcl.UserData> ret = (acl != null ? acl.listUsers() : null);
        return ret;
           
    }

    @Override
    public Map<String, Map<String, Set<DatasetACL.aclId>>> listGroups() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, Map<String, Set<DatasetACL.aclId>>> ret = (acl != null ? acl.listGroups() : null);
        return ret;

    }

    @Override
    public SEPAAcl.UserData viewUser(String name) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final SEPAAcl.UserData ret = (acl != null ? acl.viewUser(name): null);
        return ret;

    }

    @Override
    public Map<String, Set<DatasetACL.aclId>> viewGroup(String name) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        final Map<String, Set<DatasetACL.aclId>> ret = (acl != null ? acl.viewGroup(name): null);
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
