package it.unibo.arces.wot.sepa.engine.processing;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

public class SpuKillerThread extends Thread {
	private final AtomicBoolean end = new AtomicBoolean(false);

	private BlockingQueue<String> queue;
	private SubscribeProcessor processor;
	
	public SpuKillerThread(BlockingQueue<String> killSpuids,SubscribeProcessor processor) {
		this.queue = killSpuids;
		this.processor = processor;
	}
	
	public void run() {
		while (!end.get()) {
			String spuid;
			try {
				spuid = queue.take();
			} catch (InterruptedException e) {
				return;
			}
			
			processor.unsubscribe(new UnsubscribeRequest(spuid));
		}
	}
	
	public void finish(){
        end.set(true);
    }
}
