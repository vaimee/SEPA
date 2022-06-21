/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa;

import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
/**
 *
 * @author Lorenzo
 */
public class MultiLineJSAP {
    public MultiLineJSAP()  {
        
    }
    
    @Test 
    public void testMultiLine() {
        try {
            final ConfigurationProvider cp = new ConfigurationProvider();
            
            final UpdateRequest ur = cp.buildAclUpdateRequest("ACL_ADD_GROUP_1");
            
            System.out.println(ur.getSPARQL());
            
            
            final UpdateRequest ur2 = cp.buildAclUpdateRequest("ACL_DELETE_ALL");
            Assertions.assertTrue(ur2.getSPARQL().contains( "DELETE WHERE { GRAPH ?g {?s ?p ?o}}"));
            
        } catch(Exception e ) {
            Assertions.fail(e);
            
        }
    }
}
