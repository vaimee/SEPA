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

/**
 *
 * @author Lorenzo
 */
public class SEPAAclProcessor implements SEPAAclProcessorMBean{
    public SEPAAclProcessor() {
        SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
    }
    @Override
    public void reloadUsers() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        if (acl != null) {
            acl.loadUsers();
        }
    }

    @Override
    public void reloadGroups() {
        final SEPAAcl acl = SEPAAcl.getInstance();
        if (acl != null) {
            acl.loadGroups();
        }
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
    public void reloadUser(String userName) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        if (acl != null) {
            acl.loadUser(userName);
        }

    }

    @Override
    public void reloadGroup(String groupName) {
        final SEPAAcl acl = SEPAAcl.getInstance();
        if (acl != null) {
            acl.loadGroup(groupName);
        }

    }

    @Override
    public void removeUser(String user) {
        methodInvoker("removeUser", new Object[] {user});
    }

    @Override
    public void removeUserPermissions(String user, String graph) {
        methodInvoker("removeUserPermissions", new Object[] {user,graph});
    }

    @Override
    public void removeUserPermission(String user, String graph, DatasetACL.aclId id) {
        methodInvoker("removeUserPermission", new Object[] {user,graph,id});
    }

    @Override
    public void addUser(String user) {
        methodInvoker("addUser", new Object[] {user});
    }

    @Override
    public void addUserPermission(String user, String graph, DatasetACL.aclId id) {
        methodInvoker("addUserPermission", new Object[] {user,graph,id});
    }

    @Override
    public void addUserToGroup(String user, String group) {
        methodInvoker("addUserToGroup", new Object[] {user,group});
    }

    @Override
    public void removeUserFromGroup(String user, String group) {
        methodInvoker("removeUserFromGroup", new Object[] {user,group});
    }

    @Override
    public void removeGroup(String group) {
        methodInvoker("removeGroup", new Object[] {group});
    }

    @Override
    public void removeGroupPermissions(String group, String graph) {
        methodInvoker("removeGroupPermissions", new Object[] {group,graph});
    }

    @Override
    public void removeGroupPermission(String group, String graph, DatasetACL.aclId id) {
        methodInvoker("removeGroupPermission", new Object[] {group,graph,id});
    }

    @Override
    public void addGroup(String group) {
        methodInvoker("addGroup", new Object[] {group});
    }

    @Override
    public void addGroupPermission(String group, String graph, DatasetACL.aclId id) {
        methodInvoker("addGroupPermission", new Object[] {group,graph,id});
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
    
    private void methodInvoker(String name, Object args[]) {
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
                }
            } catch(Exception e ) {
                System.err.println(e);
            }
        }
    }
    
    
    
    public static void main(String[] args ) {
        final Object a[]  =  { "parama1",12};
        for(final Object o : a) {
            System.out.println(o.getClass().getName());
    }
    }
}
