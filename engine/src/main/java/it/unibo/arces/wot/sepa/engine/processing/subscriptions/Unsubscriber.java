package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unsubscriber thread. It loops over Unsubcribe Request queue and removes the SPU from
 * SpuManager
 * 
 * @see SPUManager
 */
public class Unsubscriber extends Thread {
    private final Logger logger = LogManager.getLogger();
    private final BlockingQueue<String> unsubscribeQueue = new LinkedBlockingQueue<String>();
    private final SPUManager spuManager;
    private final AtomicBoolean end = new AtomicBoolean(false);

    public Unsubscriber(SPUManager manager){
        super("SEPA-SPU-Unsubscriber");
        //this.unsubscribeQueue = unsubscribeQueue;
        spuManager = manager;
    }

    @Override
    public void run() {
        while (!end.get()) {
            String spuUID = null;
            try {
                spuUID = unsubscribeQueue.take();
                logger.debug("Terminating: " + spuUID);

                spuManager.unRegister(spuUID);

                SubscribeProcessorBeans.setActiveSPUs(spuManager.size());
                logger.debug("Active SPUs: " + spuManager.size());
            } catch (InterruptedException e) {
                logger.debug(e);
            }
        }
    }
    
    public void deactivate(String spuid) throws InterruptedException {
    		unsubscribeQueue.put(spuid);
    }

    public void finish(){
        end.set(true);
    }
}


