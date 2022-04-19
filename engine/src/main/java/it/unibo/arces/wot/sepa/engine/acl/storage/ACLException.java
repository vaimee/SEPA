/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

/**
 *
 * @author Lorenzo
 */
public class ACLException extends Exception {
    public ACLException() {
        super();
    }
    public ACLException(String msg) {
        super(msg);
    }
    public ACLException(String msg, Exception inner) {
        super(msg,inner);
    }
}
