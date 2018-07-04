package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpuManager is a monitor class. It takes care of the SPU collection and it
 * encapsulates filtering algorithms based on the internal structure.
 */
public class SPUManager {
	private final Logger logger = LogManager.getLogger();

	// Registered SPUs
	private HashMap<String, ISPU> spus = new HashMap<>();

	// SPUs processing set
	private HashSet<ISPU> processingSpus = new HashSet<>();

	public void startProcessing(UpdateResponse update) {
		synchronized (processingSpus) {
			processingSpus.clear();

			Iterator<ISPU> spus = filter(update);

			while (spus.hasNext()) {
				ISPU spu = spus.next();
				processingSpus.add(spu);
				spu.process(update);
			}

			logger.debug("Activated SPUs: " + processingSpus.size());
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

	public synchronized void register(ISPU spu) {
		synchronized (processingSpus) {
			spus.put(spu.getUUID(), spu);
		}
	}

	public synchronized void unRegister(String spuID) {
		synchronized (processingSpus) {
			if (!isValidSpuId(spuID)) {
				throw new IllegalArgumentException("Unregistering a not existing SPUID: " + spuID);
			}
			spus.get(spuID).terminate();
			spus.remove(spuID);
		}
	}

	public synchronized boolean isValidSpuId(String id) {
		return spus.containsKey(id);
	}

	public synchronized Iterator<ISPU> filter(UpdateResponse response) {
		return spus.values().iterator();
	}

	public synchronized Collection<ISPU> getAll() {
		return spus.values();
	}

	public synchronized int size() {
		return spus.values().size();
	}

}
