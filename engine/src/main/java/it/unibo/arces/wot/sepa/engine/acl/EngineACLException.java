/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl;

import org.apache.jena.acl.ACLException;

/**
 *
 * @author Lorenzo
 */
public class EngineACLException extends ACLException {
    public EngineACLException() {
        super();
    }
    public EngineACLException(String msg) {
        super(msg);
    }
    public EngineACLException(String msg, Exception inner) {
        super(msg,inner);
    }
}
