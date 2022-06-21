/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
/**
 *
 * @author Lorenzo
 */
public interface ACLStorageRegistrable {
    void register(ACLStorageRegistrableParams params,ACLStorage owner);
    void registerSecure(ACLStorageRegistrableParams params,ACLStorage owner);
}
