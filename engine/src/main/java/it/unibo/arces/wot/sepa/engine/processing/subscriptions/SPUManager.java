package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class SPUManager {
    private HashMap<String,ISubscriptionProcUnit> spus = new HashMap<>();

    public synchronized void Register(ISubscriptionProcUnit spu){
        spus.put(spu.getUUID(),spu);
    }

    public synchronized void UnRegister(String spuID){
        if(!isValidSPUID(spuID)){
            throw new IllegalArgumentException("Unregistering a not existing SPUID: "+ spuID);
        }
        spus.get(spuID).terminate();
        spus.remove(spuID);
    }

    public synchronized boolean isValidSPUID(String id){
        return spus.containsKey(id);
    }

    public synchronized Iterator<ISubscriptionProcUnit> Filter(UpdateRequest request){
        return spus.values().iterator();
    }

    public synchronized Collection<ISubscriptionProcUnit> GetAll(){
        return spus.values();
    }

    public synchronized int size(){
        return spus.values().size();
    }


}
