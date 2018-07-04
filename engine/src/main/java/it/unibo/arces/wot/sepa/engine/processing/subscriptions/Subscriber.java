package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Subscriber extends Thread {
    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean end = new AtomicBoolean(false);
    private final BlockingQueue<ISPU> subscriptionQueue = new LinkedBlockingQueue<ISPU>();
    private final SPUManager spuManager;

    //private LinkedBlockingQueue<ISPU> subscribeQueue = new LinkedBlockingQueue<>();
    
    //public Subscriber(BlockingQueue<ISPU> subscriptionQueue, SPUManager manager){
    	public Subscriber(SPUManager manager){
        super("SEPA-SPU-Subscriber");
        //this.subscriptionQueue = subscriptionQueue;
        spuManager = manager;
    }

    @Override
    public void run() {
        while (!end.get()) {
            ISPU spu = null;
            try {
            		// Wait for a new SPU to be activated
                spu = subscriptionQueue.take();
                
                // Start the SPU thread
                Thread th = new Thread(spu);
                th.setName("SPU_" + spu.getUUID());
                th.start();

                spuManager.register(spu);

                SubscribeProcessorBeans.setActiveSPUs(spuManager.size());
                logger.debug(spu.getUUID() + " ACTIVATED (total: " + spuManager.size() + ")");
            } catch (InterruptedException e) {
                logger.debug(e);
            }
        }
    }

    public void activate(ISPU spu) throws InterruptedException {
    		subscriptionQueue.put(spu);
    }
    
    public void finish(){
        end.set(true);
    }
}
