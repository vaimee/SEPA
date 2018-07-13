package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpuManager is a monitor class. It takes care of the SPU collection and it
 * encapsulates filtering algorithms based on the internal structure.
 */
public class SPUManager {
	private final Logger logger = LogManager.getLogger();

	// SPUID ==> SPU 
	private final HashMap<String, ISPU> spus = new HashMap<>();

	// Request ==> SPU
	private final HashMap<SubscribeRequest, ISPU> request2Spu = new HashMap<>();
	
	// SPUID ==> Request
	private final HashMap<String, SubscribeRequest> spuid2Request = new HashMap<>();
	
	// SPUs processing set
	private final HashSet<ISPU> processingSpus = new HashSet<>();

	private final Subscriber subscriber;
	private final Unsubscriber unsubscriber;
	
	public SPUManager() {
		this.subscriber = new Subscriber(this);
		this.unsubscriber = new Unsubscriber(this);	
	}
	
	public void start() {
		this.subscriber.start();
		this.unsubscriber.start();
	}

	public void stop() {
		this.subscriber.finish();
		this.unsubscriber.finish();

		this.subscriber.interrupt();
		this.unsubscriber.interrupt();
	}
	
	public String generateSpuid() {
		return "sepa://spuid/" + UUID.randomUUID().toString();
	}

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

	public void endProcessing(SPU s) {
		synchronized (processingSpus) {
			processingSpus.remove(s);
			logger.debug("SPUs left: " + processingSpus.size());
			processingSpus.notify();
		}
	}

	public boolean isEmpty() {
		return processingSpus.isEmpty();
	}

	public synchronized void register(ISPU spu, SubscribeRequest request) {
		logger.debug("Register SPU: "+spu.getUUID());
		
		spus.put(spu.getUUID(), spu);
		request2Spu.put(request, spu);
		spuid2Request.put(spu.getUUID(), request);
	}

	public synchronized void unRegister(String spuID) {
		if (!isValidSpuId(spuID)) {
			throw new IllegalArgumentException("Unregistering a not existing SPUID: " + spuID);
		}
		
		spus.get(spuID).terminate();
		
		spus.remove(spuID);
		request2Spu.remove(spuid2Request.get(spuID));
		spuid2Request.remove(spuID);
	}

	private boolean isValidSpuId(String id) {
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

	public synchronized ISPU getSPU(SubscribeRequest req) {
		return request2Spu.get(req);
	}

	public boolean activate(ISPU spu, SubscribeRequest req) {
		try {
			subscriber.activate(spu, req);
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	public boolean deactivate(String spuid) {
		try {
			unsubscriber.deactivate(spuid);
		} catch (InterruptedException e) {
			return false;
		}
		return true;	
	}

}
