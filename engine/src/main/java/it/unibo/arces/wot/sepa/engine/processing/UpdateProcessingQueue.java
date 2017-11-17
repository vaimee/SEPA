package it.unibo.arces.wot.sepa.engine.processing;

import java.util.concurrent.ConcurrentLinkedQueue;

public class UpdateProcessingQueue {
	private ConcurrentLinkedQueue<SPUEndOfProcessing> responses = new ConcurrentLinkedQueue<SPUEndOfProcessing>();
	
	public void waitUpdateEOP() throws InterruptedException {
		while(responses.poll()==null) {
			synchronized(responses) {
				responses.wait();
			}
		}
	}
	
	public void updateEOP(SPUEndOfProcessing eop) {
		responses.add(eop);
		synchronized(responses) {
			responses.notify();
		}
	}
}
