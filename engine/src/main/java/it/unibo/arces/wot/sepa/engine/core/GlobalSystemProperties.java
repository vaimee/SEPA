/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.core;

/**
 *
 * @author Lorenzo
 */
public class GlobalSystemProperties {
    public static final String TEST_ACL = "testACL";
    
    public static boolean checkSystemProperty(final String name) {
        final String s = System.getProperty(name);
        return s != null;
    }
    
    public static boolean checkIfACLIntegrationTest() {
        return checkSystemProperty(TEST_ACL);
    }
}
