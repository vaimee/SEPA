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
import it.unibo.arces.wot.sepa.engine.processing.SEPAAclProcessorMBean;

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
    
}
