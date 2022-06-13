/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalRequest;

/**
 *
 * @author Lorenzo
 */
public class SEPAUserInfo {
    public final String userName;
    
    private static boolean  hasACLSupport;
    private static boolean  hasInit;
    private static boolean  hasAclDebug;
    
    public static SEPAUserInfo newInstance(InternalRequest req) throws SEPASecurityException {
        if (hasInit == false) {
            hasACLSupport = EngineBeans.isAclEnabled();
            hasAclDebug  = System.getProperty("acl.debug") != null;
            hasInit = true;
            
            System.out.println("hasACLSupport : " + hasACLSupport);
            System.out.println("hasAclDebug : " + hasAclDebug);
        }
        
        if (hasACLSupport == false)
            return null;
        
        SEPAUserInfo  ret = null;
        if (    req != null                                                 && 
                req.getClientAuthorization() != null                        &&
                req.getClientAuthorization().getCredentials()  != null

        ) {
            ret  = new SEPAUserInfo(req.getClientAuthorization().getCredentials().user());
        }
        
        if ((ret == null || ret.validate() == false) && hasAclDebug == false   ) {
            throw new SEPASecurityException("Unable to find user information for ACL check");
        }
        
        return ret;
        
    }
    
    private SEPAUserInfo(String n) {
        userName = n;
    }
    
    public boolean validate() {
        return userName != null;
    }
} 
