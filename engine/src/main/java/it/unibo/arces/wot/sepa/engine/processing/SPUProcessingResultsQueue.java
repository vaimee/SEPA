package it.unibo.arces.wot.sepa.engine.processing;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SPUProcessingResultsQueue {
	private ConcurrentLinkedQueue<SPU> responses = new ConcurrentLinkedQueue<SPU>();
	
	public SPU waitSPUEndOfProcessing() throws InterruptedException {
		SPU spu;
		while((spu=responses.poll())==null) {
			synchronized(responses) {
				responses.wait();
			}
		}
		return spu;
	}
	
	public void endOfProcessing(SPU spu) {
		responses.add(spu);
		synchronized(responses) {
			responses.notify();
		}
	}
}
