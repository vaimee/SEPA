/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.processing.endpoint.SPARQLEndpoint;
import org.apache.jena.acl.DatasetACL;

/**
 *
 * @author Lorenzo
 */
public interface ACLStorageOperations extends ACLStorage,SPARQLEndpoint ,ACLStorageRegistrable {
    void    addGraphToUser(String user, String graph,DatasetACL.aclId firstId);
    void    addGraphToGroup(String group, String graph,DatasetACL.aclId firstId);
    
    
}
