/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unibo.arces.wot.sepa.engine.acl.storage;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;

/**
 *
 * @author Lorenzo
 */
public class ACLStorageRegistrableParams {
    public final ServerBootstrap    sp;
    public final Scheduler          scheduler;
    
    public ACLStorageRegistrableParams(ServerBootstrap sp, Scheduler sched) {
        this.sp = sp;
        this.scheduler = sched;
    }
}
