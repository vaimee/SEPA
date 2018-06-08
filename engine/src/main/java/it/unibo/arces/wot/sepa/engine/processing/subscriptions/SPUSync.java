package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import java.util.Collection;
import java.util.HashSet;

import it.unibo.arces.wot.sepa.engine.processing.subscriptions.ISubscriptionProcUnit;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPU;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

public class SPUSync {
	private final Logger logger = LogManager.getLogger();

	// SPU synchronization
	private HashSet<ISubscriptionProcUnit> processingSpus = new HashSet<>();

	public void startProcessing(Collection<ISubscriptionProcUnit> spus) {
		synchronized (processingSpus) {
			processingSpus.clear();
			processingSpus.addAll(spus);
		}
	}

	public void waitEndOfProcessing() {
		// Wait all SPUs completing processing (or timeout)
		synchronized (processingSpus) {
			while (!processingSpus.isEmpty()) {
				logger.debug(String.format("Wait (%s) SPUs to complete processing...", processingSpus.size()));
				try {
					processingSpus.wait(SubscribeProcessorBeans.getSPUProcessingTimeout());
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
