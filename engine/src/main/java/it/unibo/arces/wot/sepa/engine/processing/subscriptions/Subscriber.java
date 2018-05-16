package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Subscriber extends Thread {
    private final Logger logger = LogManager.getLogger("Subscriber");
    private final AtomicBoolean end = new AtomicBoolean(false);
    private final BlockingQueue<ISubscriptionProcUnit> subscriptionQueue;
    private final SPUManager spuManager;

    public Subscriber(BlockingQueue<ISubscriptionProcUnit> subscriptionQueue, SPUManager manager){
        super("SEPA SPU Subscriber");
        this.subscriptionQueue = subscriptionQueue;
        spuManager = manager;
    }

    @Override
    public void run() {
        while (!end.get()) {
            ISubscriptionProcUnit spu = null;
            try {
                spu = subscriptionQueue.take();
                // Start the SPU thread
                Thread th = new Thread(spu);
                th.setName("SPU_" + spu.getUUID());
                th.start();

                spuManager.register(spu);

                SPUManagerBeans.setActiveSPUs(spuManager.size());
                logger.debug(spu.getUUID() + " ACTIVATED (total: " + spuManager.size() + ")");
            } catch (InterruptedException e) {
                logger.info(e);
            }
        }
    }

    public void finish(){
        end.set(true);
    }
}
