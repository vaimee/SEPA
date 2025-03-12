/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.acl.EngineACLException;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl;
import it.unibo.arces.wot.sepa.engine.acl.SEPAAcl.UserData;
import java.util.Map;
import java.util.Set;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public interface ACLStorageListable {
    Map<String, SEPAAcl.UserData>                   listUsers() throws EngineACLException;
    Map<String,Map<String, Set<DatasetACL.aclId>>>  listGroups() throws EngineACLException;
    UserData                                        viewUser(final String userName) throws EngineACLException;
    Map<String, Set<DatasetACL.aclId>>              viewGroup(final String groupName) throws EngineACLException;
}
