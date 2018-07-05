package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

class Subscriber extends Thread {
    private final Logger logger = LogManager.getLogger();
    private final AtomicBoolean end = new AtomicBoolean(false);
    
    private final BlockingQueue<ISPU> subscriptionQueue = new LinkedBlockingQueue<ISPU>();
    private final BlockingQueue<SubscribeRequest> requestQueue = new LinkedBlockingQueue<SubscribeRequest>();
    
    private final SPUManager spuManager;

    	public Subscriber(SPUManager manager){
        super("SEPA-SPU-Subscriber");
        spuManager = manager;
    }

    @Override
    public void run() {
        while (!end.get()) {
            try {
            		// Wait for a new SPU to be activated
            		ISPU spu = subscriptionQueue.take();
                SubscribeRequest request = requestQueue.take();
                
                // Start the SPU thread
                logger.debug("Starting SPU: "+spu.getUUID());
                Thread th = new Thread(spu);
                th.setName("SPU_" + spu.getUUID());
                th.start();
                logger.debug("Started SPU: "+spu.getUUID());
                
                spuManager.register(spu,request);

                SubscribeProcessorBeans.setActiveSPUs(spuManager.size());
                logger.debug(spu.getUUID() + " ACTIVATED (total: " + spuManager.size() + ")");
            } catch (InterruptedException e) {
                logger.debug(e);
            }
        }
    }

    public void activate(ISPU spu,SubscribeRequest request) throws InterruptedException {
    		subscriptionQueue.put(spu);
    		requestQueue.put(request);
    }
    
    public void finish(){
        end.set(true);
    }
}
