/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl;

/**
 *
 * @author Lorenzo
 */
public class SEPAUserInfo {
    public final String userName;
    
    public static SEPAUserInfo newInstance(String userName) {
        return new SEPAUserInfo(userName);
    }
    
    private SEPAUserInfo(String n) {
        userName = n;
    }
} 
