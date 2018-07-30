package it.unibo.arces.wot.sepa.engine.processing;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPU;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUnsubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

class SubscribeProcessingThread extends Thread implements SubscribeProcessingThreadMBean, EventHandler {
	private static final Logger logger = LogManager.getLogger();

	private final Processor processor;

	// Maps
	private static final HashMap<String, ArrayList<String>> activeSpus = new HashMap<String, ArrayList<String>>();
	private static final HashMap<String, String> spuids = new HashMap<String, String>();
	private static final HashMap<String, EventHandler> handlers = new HashMap<String, EventHandler>();
	private static final HashMap<String, Integer> sequenceNumbers = new HashMap<String, Integer>();

	// Broken SPUs disposer
	private final Thread killer;

	public SubscribeProcessingThread(Processor processor) {
		this.processor = processor;

		setName("SEPA-Subscribe-Processor");

		killer = new Thread() {
			public void run() {
				while (processor.isRunning()) {
					try {
						String spuid = processor.getSchedulerQueue().waitSpuid2Kill();
						synchronized (activeSpus) {
							unregisterHandler(spuids.get(spuid), spuid);
						}
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		killer.setName("SEPA-SPU-Killer");
		
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	@Override
	public void start() {
		killer.start();
		super.start();
	}

	public void run() {
		while (processor.isRunning()) {
			try {
				// Wait request...
				ScheduledRequest request = processor.getSchedulerQueue().waitSubscribeUnsubscribeRequest();
				logger.debug(">> "+request);

				// Process request
				Response response = null;
				if (request.isSubscribeRequest())
					response = subscribe((InternalSubscribeRequest) request.getRequest());
				else if (request.isUnsubscribeRequest())
					response = unsubscribe(((InternalUnsubscribeRequest) request.getRequest()).getSpuid());

				logger.debug("<< "+response);
				
				// Send back response
				processor.getSchedulerQueue().addResponse(request.getToken(), response);

			} catch (InterruptedException e) {
				killer.interrupt();
				return;
			}
		}
	}

	private Response subscribe(InternalSubscribeRequest req) {
		SubscribeProcessorBeans.subscribeRequest();
		
		EventHandler eventHandler = req.getEventHandler();
		String sparql = req.getSparql();
		String alias = req.getAlias();
		String defaultGraph = req.getDefaultGraphUri();
		String namedGraph = req.getNamedGraphUri();
		InternalSubscribeRequest wrappedRequest = new InternalSubscribeRequest(sparql, alias, defaultGraph, namedGraph,
				this);

		// Get an SPU from the SPU manager (already available or a new one)
		SPU spu = processor.getSPUManager().getSPU(wrappedRequest);
		if (spu == null)
			return new ErrorResponse(500, "internal_server_error", "Failed to create SPU");

		// Generate a fake SPU id
		String spuid = processor.getSPUManager().generateSpuid();

		// Register handler
		registerHandler(spu.getUUID(), spuid, eventHandler);

		return new SubscribeResponse(spuid, req.getAlias(), spu.getLastBindings());
	}

	private Response unsubscribe(String spuid) {
		SubscribeProcessorBeans.unsubscribeRequest();
		
		synchronized (activeSpus) {	
			String masterSpuid = spuids.get(spuid);
			logger.debug("Master spuid: " + masterSpuid + " (" + spuid + ")");

			if (masterSpuid == null)
				return new ErrorResponse(404, "spuid_not_found", "SPUID not found: " + spuid);

			// Unregister handler
			unregisterHandler(masterSpuid, spuid);
		}

		return new UnsubscribeResponse(spuid);
	}

	public void killSpu(String spuid) {
		synchronized (activeSpus) {
			unregisterHandler(spuids.get(spuid), spuid);
		}
	}

	@Override
	public void notifyEvent(Notification notify) {
		// synchronized (handlers) {
		logger.debug("@notifyEvent: " + notify);

		String spuid = notify.getSpuid();

		ArrayList<String> toBeKilled = new ArrayList<String>();

		synchronized (activeSpus) {
			if (activeSpus.containsKey(spuid)) {
				for (String client : activeSpus.get(spuid)) {
					try {
						// Dispatching events
						Notification event = new Notification(client, notify.getARBindingsResults(),
								sequenceNumbers.get(client));
						handlers.get(client).notifyEvent(event);
						sequenceNumbers.put(client, sequenceNumbers.get(client) + 1);
					} catch (Exception e) {
						logger.error("@notifyEvent Client:" + client + " Notification: " + notify + " Exception:"
								+ e.getMessage());

						// Handler is gone: unregister it
						toBeKilled.add(client);
					}
				}

				for (String client : toBeKilled)
					unregisterHandler(spuid, client);
			}
		}
	}

	private void registerHandler(String masterSpuid, String spuid, EventHandler handler) {
		logger.debug("Register SPU handler: " + spuid);

		SubscribeProcessorBeans.registerHandler();
		
		synchronized (activeSpus) {
			if (activeSpus.get(masterSpuid) == null)
				activeSpus.put(masterSpuid, new ArrayList<String>());
			activeSpus.get(masterSpuid).add(spuid);
			
			handlers.put(spuid, handler);
			sequenceNumbers.put(spuid, 1);
			spuids.put(spuid, masterSpuid);
		}
	}

	private void unregisterHandler(String masterSpuid, String spuid) {
		logger.debug("Unregister SPU handler: " + spuid);

		SubscribeProcessorBeans.unregisterHandler();
		
		// SPUids
		synchronized (activeSpus) {
			spuids.remove(spuid);
			sequenceNumbers.remove(spuid);
			handlers.remove(spuid);
			
			if (!activeSpus.containsKey(masterSpuid)) return;

			activeSpus.get(masterSpuid).remove(spuid);
			logger.debug(masterSpuid + " number of clients: " + activeSpus.get(masterSpuid).size());
			if (activeSpus.get(masterSpuid).isEmpty()) {
				activeSpus.remove(masterSpuid);

				// Deactivate SPU
				processor.getSPUManager().deactivate(masterSpuid);
			}
		}
	}

	@Override
	public long getUpdateRequests() {
		return SubscribeProcessorBeans.getUpdateRequests();
	}

	@Override
	public long getSPUs_current() {
		return SubscribeProcessorBeans.getSPUs_current();
	}

	@Override
	public long getSPUs_max() {
		return SubscribeProcessorBeans.getSPUs_max();
	}

	@Override
	public float getSPUs_time() {
		return SubscribeProcessorBeans.getSPUs_time();
	}

	@Override
	public void reset() {
		SubscribeProcessorBeans.reset();
	}

	@Override
	public float getSPUs_time_min() {
		return SubscribeProcessorBeans.getSPUs_time_min();
	}

	@Override
	public float getSPUs_time_max() {
		return SubscribeProcessorBeans.getSPUs_time_max();
	}

	@Override
	public float getSPUs_time_average() {
		return SubscribeProcessorBeans.getSPUs_time_averaae();
	}

	@Override
	public long getSubscribeRequests() {
		return SubscribeProcessorBeans.getSubscribeRequests();
	}

	@Override
	public long getUnsubscribeRequests() {
		return SubscribeProcessorBeans.getUnsubscribeRequests();
	}

	@Override
	public long getSPUProcessingTimeout() {
		return SubscribeProcessorBeans.getSPUProcessingTimeout();
	}

	@Override
	public void setSPUProcessingTimeout(long t) {
		SubscribeProcessorBeans.setActiveSPUs(t);
	}

	@Override
	public void scale_ms() {
		SubscribeProcessorBeans.scale_ms();
	}

	@Override
	public void scale_us() {
		SubscribeProcessorBeans.scale_us();
	}

	@Override
	public void scale_ns() {
		SubscribeProcessorBeans.scale_ns();
	}

	@Override
	public String getUnitScale() {
		return SubscribeProcessorBeans.getUnitScale();
	}

	@Override
	public long getSubscribers() {
		return SubscribeProcessorBeans.getSubscribers(); 
	}

	@Override
	public long getSubscribers_max() {
		return SubscribeProcessorBeans.getSubscribersMax(); 
	}
}
