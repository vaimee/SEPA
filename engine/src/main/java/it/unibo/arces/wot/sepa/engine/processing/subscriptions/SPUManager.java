package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPANotExistsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.processing.Processor;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SpuManager is a monitor class. It takes care of the SPU collection and it
 * encapsulates filtering algorithms based on the internal structure.
 */
public class SPUManager implements SPUManagerMBean, EventHandler {
	private final Logger logger = LogManager.getLogger();

	// SPUs processing pool
	private final HashSet<SPU> processingPool = new HashSet<SPU>();
	private Collection<SPU> activeSpus;
	private Semaphore processingMutex = new Semaphore(1, true);

	// SPUID ==> SPU
	private final HashMap<String, SPU> spus = new HashMap<String, SPU>();

	private final Processor processor;

	public SPUManager(Processor processor) {
		this.processor = processor;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	// TODO: choose different kinds of SPU based on subscribe request
	protected SPU createSPU(InternalSubscribeRequest req, SPUManager manager) {
		try {
			return new SPUNaive(req, this);
		} catch (SEPAProtocolException e) {
			return null;
		}
	}

	// TODO: filtering SPUs to be activated & check if there are "zombie" SPUs
	protected Collection<SPU> filter(InternalUpdateRequest update) {
		Collection<SPU> all = spus.values();
		Collection<SPU> ret = new ArrayList<SPU>();
		
		for (SPU spu : all) {
			if (Subscriptions.isZombieSpu(spu.getSPUID())) {
				spu.finish();
				spu.interrupt();	
				synchronized(spus) {
					spus.remove(spu.getSPUID());
				}
			}
			else {
				ret.add(spu);
			}
		}
		
		return ret;
	}

	public synchronized void preUpdateProcessing(InternalUpdateRequest update) throws SEPAProcessingException {
		try {
			processingMutex.acquire();
		} catch (InterruptedException e) {
			throw new SEPAProcessingException(e);
		}

		logger.debug("*** PRE PROCESSING SUBSCRIPTIONS BEGIN *** ");

		long start = Timings.getTime();

		// Get active SPUs (e.g., LUTT filtering)
		synchronized (spus) {
			activeSpus = filter(update);
		}
		long stop = Timings.getTime();

		SPUManagerBeans.filteringTimings(start, stop);

		start = Timings.getTime();

		// Copy active SPU pool
		processingPool.clear();

		synchronized (activeSpus) {
			for (SPU spu : activeSpus) {
				processingPool.add(spu);
				spu.preUpdateProcessing(update);
			}
		}

		logger.debug("@preUpdateProcessing SPU processing pool size: " + processingPool.size());

		// Wait all SPUs to complete processing
		if (!processingPool.isEmpty()) {
			logger.debug(String.format("@preUpdateProcessing wait (%d ms) for %d SPUs to complete processing...",
					SPUManagerBeans.getSPUProcessingTimeout(), processingPool.size()));
			try {
				wait(SPUManagerBeans.getSPUProcessingTimeout());
			} catch (InterruptedException e) {
				processingMutex.release();
				throw new SEPAProcessingException(e);
			}
		}

		// Pre processing not completed
		if (!processingPool.isEmpty()) {
			logger.error(
					"@preUpdateProcessing timeout on SPU processing. SPUs still running: " + processingPool.size());
			for (SPU spu : processingPool) {
				logger.error("@preUpdateProcessing zombie spuid: " + spu.getSPUID());
			}
		}

		stop = Timings.getTime();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** PRE PROCESSING SUBSCRIPTIONS END *** ");
	}

	public synchronized void postUpdateProcessing(Response ret) throws SEPAProcessingException {
		logger.debug("*** POST PROCESSING SUBSCRIPTIONS BEGIN *** ");

		long start = Timings.getTime();

		processingPool.clear();

		synchronized (activeSpus) {
			for (SPU spu : activeSpus) {
				processingPool.add(spu);
				spu.postUpdateProcessing(ret);
			}
		}

		logger.debug("@postUpdateProcessing SPU processing pool size: " + processingPool.size());

		if (!processingPool.isEmpty()) {
			logger.debug(String.format("@postUpdateProcessing wait (%d ms) for %d SPUs to complete processing...",
					SPUManagerBeans.getSPUProcessingTimeout(), processingPool.size()));
			try {
				wait(SPUManagerBeans.getSPUProcessingTimeout());
			} catch (InterruptedException e) {
				processingMutex.release();
				throw new SEPAProcessingException(e);
			}
		}

		// TIMEOUT
		if (!processingPool.isEmpty()) {
			logger.error(
					"@postUpdateProcessing timeout on SPU processing. SPUs still running: " + processingPool.size());
			for (SPU spu : processingPool) {
				logger.error("@postUpdateProcessing zombie spuid: " + spu.getSPUID());
			}
		}

		long stop = Timings.getTime();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** POST PROCESSING SUBSCRIPTIONS END *** ");

		processingMutex.release();
	}

	public synchronized void endOfProcessing(SPU s) {
		logger.debug("@endOfProcessing  SPUID: " + s.getSPUID());

		processingPool.remove(s);
		if (processingPool.isEmpty())
			notify();
	}

	public void exceptionOnProcessing(SPU s) {
		logger.error("@exceptionOnProcessing  SPUID: " + s.getSPUID());

		activeSpus.remove(s);
		endOfProcessing(s);
	}

	public Response subscribe(InternalSubscribeRequest req) throws SEPAProcessingException {
		try {
			processingMutex.acquire();
		} catch (InterruptedException e) {
			throw new SEPAProcessingException(e);
		}

		SPUManagerBeans.subscribeRequest();

		// Set the SPU Manager as event handler
		String sparql = req.getSparql();
		String alias = req.getAlias();
		String defaultGraph = req.getDefaultGraphUri();
		String namedGraph = req.getNamedGraphUri();
		InternalSubscribeRequest wrappedRequest = new InternalSubscribeRequest(sparql, alias, defaultGraph, namedGraph,
				this);

		// Create or link to an existing SPU
		SPU spu;
		if (Subscriptions.contains(req)) {
			spu = Subscriptions.getSPU(req);
		} else {
			spu = createSPU(wrappedRequest, this);

			// Initialize SPU
			Response init = spu.init();
			if (init.isError()) {
				logger.error("@subscribe SPU initialization failed: " + init);
				if (alias != null) {
					((ErrorResponse) init).setAlias(alias);
				}
				processingMutex.release();
				return init;
			}

			// Register request
			Subscriptions.register(req, spu);

			// Create new entry for handler
			synchronized (spus) {
				spus.put(spu.getSPUID(), spu);
			}

			// Start the SPU thread
			spu.setName(spu.getSPUID());
			spu.start();
		}

		Subscriber sub = Subscriptions.addSubscriber(req, spu);

		processingMutex.release();

		return new SubscribeResponse(sub.getSID(), req.getAlias(), sub.getSPU().getLastBindings());
	}

	public Response unsubscribe(String sid, String gid) throws SEPAProcessingException {
		return internalUnsubscribe(sid, gid, true);
	}

	public void killSubscription(String sid, String gid) throws SEPAProcessingException {
		internalUnsubscribe(sid, gid, false);
	}

	private Response internalUnsubscribe(String sid, String gid, boolean dep) throws SEPAProcessingException {
		try {
			processingMutex.acquire();
		} catch (InterruptedException e) {
			throw new SEPAProcessingException(e);
		}

		try {
			Subscriber sub = Subscriptions.getSubscriber(sid);
			String spuid = sub.getSPU().getSPUID();
			
			if (Subscriptions.removeSubscriber(sub)) {
				// If it is the last handler: kill SPU
				spus.get(spuid).finish();
				spus.get(spuid).interrupt();

				// Clear
				synchronized (spus) {
					spus.remove(spuid);
				}

				logger.info("@internalUnsubscribe active SPUs: " + spus.size());
				SPUManagerBeans.setActiveSPUs(spus.size());
			}
		} catch (SEPANotExistsException e) {
			logger.warn("@internalUnsubscribe SID not found: " + sid);
			return new ErrorResponse(500, "sid_not_found", "Unregistering a not existing subscriber: " + sid);
		}

		if (dep)
			Dependability.onUnsubscribe(gid, sid);

		processingMutex.release();

		return new UnsubscribeResponse(sid);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		logger.debug("@notifyEvent " + notify);

		String spuid = notify.getSpuid();

		synchronized (spus) {
			if (spus.containsKey(spuid)) {
				Subscriptions.notifySubscribers(spuid, notify);
			}
		}
	}

	public QueryProcessor getQueryProcessor() {
		return processor.getQueryProcessor();
	}

	@Override
	public long getUpdateRequests() {
		return SPUManagerBeans.getUpdateRequests();
	}

	@Override
	public long getSPUs_current() {
		return SPUManagerBeans.getSPUs_current();
	}

	@Override
	public long getSPUs_max() {
		return SPUManagerBeans.getSPUs_max();
	}

	@Override
	public float getSPUs_time() {
		return SPUManagerBeans.getSPUs_time();
	}

	@Override
	public void reset() {
		SPUManagerBeans.reset();
	}

	@Override
	public float getSPUs_time_min() {
		return SPUManagerBeans.getSPUs_time_min();
	}

	@Override
	public float getSPUs_time_max() {
		return SPUManagerBeans.getSPUs_time_max();
	}

	@Override
	public float getSPUs_time_average() {
		return SPUManagerBeans.getSPUs_time_average();
	}

	@Override
	public long getSubscribeRequests() {
		return SPUManagerBeans.getSubscribeRequests();
	}

	@Override
	public long getUnsubscribeRequests() {
		return SPUManagerBeans.getUnsubscribeRequests();
	}

	@Override
	public long getSPUProcessingTimeout() {
		return SPUManagerBeans.getSPUProcessingTimeout();
	}

	@Override
	public void setSPUProcessingTimeout(long t) {
		SPUManagerBeans.setActiveSPUs(t);
	}

	@Override
	public void scale_ms() {
		SPUManagerBeans.scale_ms();
	}

	@Override
	public void scale_us() {
		SPUManagerBeans.scale_us();
	}

	@Override
	public void scale_ns() {
		SPUManagerBeans.scale_ns();
	}

	@Override
	public String getUnitScale() {
		return SPUManagerBeans.getUnitScale();
	}

	@Override
	public long getSubscribers() {
		return SPUManagerBeans.getSubscribers();
	}

	@Override
	public long getSubscribers_max() {
		return SPUManagerBeans.getSubscribersMax();
	}

	@Override
	public float getFiltering_time() {
		return SPUManagerBeans.getFiltering_time();
	}

	@Override
	public float getFiltering_time_min() {
		return SPUManagerBeans.getFiltering_time_min();
	}

	@Override
	public float getFiltering_time_max() {
		return SPUManagerBeans.getFiltering_time_max();
	}

	@Override
	public float getFiltering_time_average() {
		return SPUManagerBeans.getFiltering_time_average();
	}
}
