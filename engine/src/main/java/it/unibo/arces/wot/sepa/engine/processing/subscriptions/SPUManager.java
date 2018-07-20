package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;
import it.unibo.arces.wot.sepa.engine.processing.Processor;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

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
	private final HashMap<String, SPU> spus = new HashMap<String, SPU>();

	// Request ==> SPU
	private final HashMap<InternalSubscribeRequest, SPU> request2Spu = new HashMap<InternalSubscribeRequest, SPU>();

	// SPUID ==> Request
	private final HashMap<String, InternalSubscribeRequest> spuid2Request = new HashMap<String, InternalSubscribeRequest>();

	// SPUs processing set
	private final HashSet<SPU> processingSpus = new HashSet<SPU>();

	private final Processor processor;

	public SPUManager(Processor processor) {
		this.processor = processor;
	}
	
	public QueryProcessor getQueryProcessor() {
		return processor.getQueryProcessor();
	}

	public String generateSpuid() {
		return "sepa://spuid/" + UUID.randomUUID().toString();
	}

	public synchronized void process(UpdateResponse update) {
		logger.debug("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
		long start = Timings.getTime();

		processingSpus.clear();

		Iterator<SPU> spus = filter(update);

		while (spus.hasNext()) {
			SPU spu = spus.next();
			processingSpus.add(spu);
			spu.process(update);
		}

		logger.debug("Activated SPUs: " + processingSpus.size());

		while (!processingSpus.isEmpty()) {
			logger.debug(String.format("Wait (%s) SPUs to complete processing...", processingSpus.size()));
			try {
				wait(SubscribeProcessorBeans.getSPUProcessingTimeout());
			} catch (InterruptedException e) {
				return;
			}
		}
		// TIMEOUT
		if (!processingSpus.isEmpty()) {
			logger.error("Timeout on SPU processing. SPUs still running: " + processingSpus.size());
		}

		long stop = Timings.getTime();

		SubscribeProcessorBeans.timings(start, stop);

		logger.debug("*** PROCESSING SUBSCRIPTIONS END *** ");
	}

	public synchronized void endProcessing(SPU s) {
		logger.debug("EOP: " + s.getUUID());
		processingSpus.remove(s);
		notify();
	}

	private Iterator<SPU> filter(UpdateResponse response) {
		return spus.values().iterator();
	}

	
	public synchronized SPU getSPU(InternalSubscribeRequest req) {
		if (request2Spu.containsKey(req))
			return request2Spu.get(req);

		// Create SPU
		SPU spu= createSPU(req,this);
		if (spu == null) {
			logger.error("SPU creation failed: "+req);
			return spu;
		}

		// Initialize SPU
		Response init = spu.init();
		if (init.isError()) {
			logger.error("SPU initialization failed");
			return null;
		}

		// Activate SPU
		activate(spu, req);

		return spu;
	}
	
	// TODO: choose different kinds of SPU based on subscribe request
	private SPU createSPU(InternalSubscribeRequest req,SPUManager manager) {
		try {
			return new SPUNaive(req, this);
		} catch (SEPAProtocolException e) {
			return null;
		}
	}

	public synchronized void activate(SPU spu, InternalSubscribeRequest request) {
		// Start the SPU thread
		logger.trace("Starting SPU: " + spu.getUUID());
		spu.setName("SPU_" + spu.getUUID());
		spu.start();

		// Register the SPU as ACTIVE
		logger.trace("Registering SPU: " + spu.getUUID());
		spus.put(spu.getUUID(), spu);
		request2Spu.put(request, spu);
		spuid2Request.put(spu.getUUID(), request);

		SubscribeProcessorBeans.setActiveSPUs(spus.values().size());
		logger.debug(spu.getUUID() + " ACTIVATED (total: " + spus.values().size() + ")");
	}

	public synchronized void deactivate(String spuid) {
		if (!spus.containsKey(spuid)) {
			logger.warn("Unregistering a not existing SPUID: " + spuid);
			return;
		}

		// Stop SPU
		spus.get(spuid).finish();
		spus.get(spuid).interrupt();
		
		// Remove from active SPUs
		spus.remove(spuid);		
		request2Spu.remove(spuid2Request.get(spuid));
		spuid2Request.remove(spuid);

		SubscribeProcessorBeans.setActiveSPUs(spus.values().size());
		logger.debug("Active SPUs: " + spus.values().size());
	}
}
