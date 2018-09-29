package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.dependability.Dependability;
import it.unibo.arces.wot.sepa.engine.processing.Processor;
import it.unibo.arces.wot.sepa.engine.processing.QueryProcessor;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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

	// SPUID ==> SPU
	private final HashMap<String, SPU> spus = new HashMap<String, SPU>();

	// SID ==> Subscriber
	private final HashMap<String, Subscriber> subscribers = new HashMap<String, Subscriber>();

	// SPU ==> Subscribers
	private final HashMap<String, HashSet<Subscriber>> handlers = new HashMap<String, HashSet<Subscriber>>();

	// Request ==> SPU
	private final HashMap<InternalSubscribeRequest, SPU> requests = new HashMap<InternalSubscribeRequest, SPU>();

	private final Processor processor;

	public SPUManager(Processor processor) {
		this.processor = processor;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public synchronized void process(UpdateResponse update) {
		logger.debug("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
		long start = Timings.getTime();

		processingPool.clear();

		Iterator<SPU> spus = filter(update);

		while (spus.hasNext()) {
			SPU spu = spus.next();
			processingPool.add(spu);
			spu.process(update);
		}

		logger.debug("@process SPU processing pool size: " + processingPool.size());

		while (!processingPool.isEmpty()) {
			logger.debug(String.format("@process  wait (%s) SPUs to complete processing...", processingPool.size()));
			try {
				wait(SPUManagerBeans.getSPUProcessingTimeout());
			} catch (InterruptedException e) {
				return;
			}
		}
		// TIMEOUT
		if (!processingPool.isEmpty()) {
			logger.error("@process timeout on SPU processing. SPUs still running: " + processingPool.size());
		}

		long stop = Timings.getTime();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** PROCESSING SUBSCRIPTIONS END *** ");
	}

	public synchronized void endOfProcessing(SPU s) {
		logger.debug("@process  EOP: " + s.getSPUID());
		processingPool.remove(s);
		notify();
	}

	public synchronized Response subscribe(InternalSubscribeRequest req) {
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
		if (requests.containsKey(req)) {
			spu = requests.get(req);
		} else {
			spu = createSPU(wrappedRequest, this);

			// Initialize SPU
			Response init = spu.init();
			if (init.isError()) {
				logger.error("@subscribe SPU initialization failed: " + init);
				return init;
			}

			// Register request
			requests.put(req, spu);

			// Create new entry for handler
			synchronized (handlers) {
				handlers.put(spu.getSPUID(), new HashSet<Subscriber>());
			}
			spus.put(spu.getSPUID(), spu);

			// Start the SPU thread
			spu.setName(spu.getSPUID());
			spu.start();

			synchronized (handlers) {
				SPUManagerBeans.setActiveSPUs(handlers.size());
			}
			logger.debug("@subscribe SPU activated: " + spu.getSPUID() + " total (" + handlers.size() + ")");
		}

		// New subscriber
		Subscriber sub = new Subscriber(spu, req.getEventHandler());
		synchronized (handlers) {
			handlers.get(spu.getSPUID()).add(sub);
		}
		subscribers.put(sub.getSID(), sub);
		
		SPUManagerBeans.addSubscriber();

		Dependability.onSubscribe(sub.getGID(), sub.getSID());

		return new SubscribeResponse(sub.getSID(), req.getAlias(), sub.getSPU().getLastBindings());
	}

	public synchronized Response unsubscribe(String sid, String gid) {
		return internalUnsubscribe(sid, gid, true);
	}

	public synchronized void killSubscription(String sid, String gid) {
		internalUnsubscribe(sid, gid, false);
	}

	private Response internalUnsubscribe(String sid, String gid, boolean dep) {
		if (!subscribers.containsKey(sid)) {
			logger.warn("@unsubscribe SID not found: " + sid);
			return new ErrorResponse(500, "sid_not_found", "Unregistering a not existing subscriber: " + sid);
		}

		// Remove subscriber
		Subscriber sub = subscribers.get(sid);
		String spuid = sub.getSPU().getSPUID();

		logger.trace("@unsubscribe SID: " + sid + " from SPU: " + spuid + " with active subscriptions: "
				+ subscribers.size());
		synchronized (handlers) {
			handlers.get(spuid).remove(sub);
		}
		subscribers.remove(sid);

		SPUManagerBeans.removeSubscriber();
		
		// No more handlers: remove SPU
		synchronized (handlers) {
			if (handlers.get(spuid).isEmpty()) {
				logger.debug("@unsubscribe no more subscribers. Kill SPU: " + sub.getSPU().getSPUID());

				// If it is the last handler: kill SPU
				spus.get(spuid).finish();
				spus.get(spuid).interrupt();

				// Clear
				spus.remove(spuid);
				requests.remove(sub.getSPU().getSubscribe());
				handlers.remove(spuid);

				logger.info("@unsubscribe active SPUs: " + spus.size());
				SPUManagerBeans.setActiveSPUs(spus.size());
			}
		}

		if (dep)
			Dependability.onUnsubscribe(gid, sid);

		return new UnsubscribeResponse(sid);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		logger.debug("@notifyEvent " + notify);

		String spuid = notify.getSpuid();

		if (spus.containsKey(spuid)) {

			synchronized (handlers) {
				for (Subscriber client : handlers.get(spuid)) {
					try {
						// Dispatching events
						Notification event = new Notification(client.getSID(), notify.getARBindingsResults(),
								client.nextSequence());
						client.getHandler().notifyEvent(event);
					} catch (Exception e) {
						logger.error("@notifyEvent " + e.getMessage());
					}
				}
			}
		}
	}

	public QueryProcessor getQueryProcessor() {
		return processor.getQueryProcessor();
	}

	// TODO: choose different kinds of SPU based on subscribe request
	private SPU createSPU(InternalSubscribeRequest req, SPUManager manager) {
		try {
			return new SPUNaive(req, this);
		} catch (SEPAProtocolException e) {
			return null;
		}
	}

	private Iterator<SPU> filter(UpdateResponse response) {
		return spus.values().iterator();
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
		return SPUManagerBeans.getSPUs_time_averaae();
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
}
