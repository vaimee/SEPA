package it.unibo.arces.wot.sepa.engine.processing;

import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;

public class SPUSync {
	private final Logger logger = LogManager.getLogger("SPUSync");

	// SPU synchronization
	private HashSet<SPU> processingSpus = new HashSet<SPU>();

	public void startProcessing(Collection<SPU> spus) {
		synchronized (processingSpus) {
			processingSpus.clear();
			processingSpus.addAll(spus);
		}
	}

	public void waitEndOfProcessing() {
		// Wait all SPUs completing processing (or timeout)
		synchronized (processingSpus) {
			while (!processingSpus.isEmpty()) {
				logger.info("Wait " + processingSpus.size() + " SPUs to complete processing...");
				try {
					processingSpus.wait(SPUManagerBeans.getSPUProcessingTimeout());
				} catch (InterruptedException e) {
					return;
				}
			}
			// TIMEOUT
			if (!processingSpus.isEmpty()) {
				logger.error("Timeout on SPU processing. SPUs still running: " + processingSpus.size());
			}
		}
	}

	public boolean isEmpty() {
		return processingSpus.isEmpty();
	}

	public void endProcessing(SPU s) {
		synchronized (processingSpus) {
			processingSpus.remove(s);
			logger.debug("SPUs left: " + processingSpus.size());
			processingSpus.notify();
		}
	}
}
