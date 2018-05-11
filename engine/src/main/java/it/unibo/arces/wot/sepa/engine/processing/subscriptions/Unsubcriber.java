package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unsubscriber thread. It loops over Unsubcribe Request queue and removes the SPU from
 * SPUManager
 * 
 * @see SPUManager
 */
public class Unsubcriber extends Thread {
    private final Logger logger = LogManager.getLogger("Unsubscriber");
    private final BlockingQueue<String> unsubscribeQueue;
    private final SPUManager spuManager;
    private final AtomicBoolean end = new AtomicBoolean(false);

    public Unsubcriber(BlockingQueue<String> unsubscribeQueue, SPUManager manager){
        super("SEPA SPU Unsubscriber");
        this.unsubscribeQueue = unsubscribeQueue;
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

                SPUManagerBeans.setActiveSPUs(spuManager.size());
                logger.debug("Active SPUs: " + spuManager.size());
            } catch (InterruptedException e) {
                logger.info(e);
            }
        }
    }

    public void finish(){
        end.set(true);
    }
}


