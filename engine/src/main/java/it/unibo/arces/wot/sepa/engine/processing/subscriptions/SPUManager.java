/* The SEPA core class for subscriptions management
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPANotExistsException;
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
	private final Semaphore processingMutex = new Semaphore(1, true);

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
		Collection<SPU> ret = new ArrayList<SPU>();

		synchronized (spus) {
			Collection<SPU> toBeRemoved = new ArrayList<SPU>();
			for (SPU spu : spus.values()) {
				if (Subscriptions.isZombieSpu(spu.getSPUID())) {
					spu.finish();
					spu.interrupt();
					toBeRemoved.add(spu);
				} else {
					ret.add(spu);
				}
			}
			for (SPU spu : toBeRemoved) {
				spus.remove(spu.getSPUID());
			}	
		}

		return ret;
	}

	public void preUpdateProcessing(InternalUpdateRequest update) throws InterruptedException {
		processingMutex.acquire();

		logger.debug("*** PRE PROCESSING SUBSCRIPTIONS BEGIN *** ");

		// Get active SPUs (e.g., LUTT filtering)
		long start = Timings.getTime();
		activeSpus = filter(update);
		long stop = Timings.getTime();
		SPUManagerBeans.filteringTimings(start, stop);

		// Processing
		start = Timings.getTime();

		synchronized (processingPool) {
			// Copy active SPU pool
			processingPool.clear();

			for (SPU spu : activeSpus) {
				processingPool.add(spu);
				spu.preUpdateProcessing(update);
			}

			logger.debug("@preUpdateProcessing SPU processing pool size: " + processingPool.size());

			// Wait all SPUs to complete processing
			if (!processingPool.isEmpty()) {
				logger.debug(String.format("@preUpdateProcessing wait (%d ms) for %d SPUs to complete processing...",
						SPUManagerBeans.getSPUProcessingTimeout(), processingPool.size()));

				processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout());
			}

			// Pre processing not completed
			if (!processingPool.isEmpty()) {
				logger.error(
						"@preUpdateProcessing TIMEOUT on SPU processing. SPUs still running: " + processingPool.size());
				for (SPU spu : processingPool) {
					logger.error("@preUpdateProcessing spuid timed out: " + spu.getSPUID());
				}
			}
		}
		
		stop = Timings.getTime();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** PRE PROCESSING SUBSCRIPTIONS END *** ");
	}

	public void postUpdateProcessing(Response ret) throws InterruptedException {
		logger.debug("*** POST PROCESSING SUBSCRIPTIONS BEGIN *** ");

		long start = Timings.getTime();

//		if (!ret.isError()) {
			synchronized (processingPool) {
				processingPool.clear();

				for (SPU spu : activeSpus) {
					processingPool.add(spu);
					spu.postUpdateProcessing(ret);
				}

				logger.debug("@postUpdateProcessing SPU processing pool size: " + processingPool.size());

				if (!processingPool.isEmpty()) {
					logger.debug(
							String.format("@postUpdateProcessing wait (%d ms) for %d SPUs to complete processing...",
									SPUManagerBeans.getSPUProcessingTimeout(), processingPool.size()));

					processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout());
				}

				// TIMEOUT
				if (!processingPool.isEmpty()) {
					logger.error("@postUpdateProcessing timeout on SPU processing. SPUs still running: "
							+ processingPool.size());
					for (SPU spu : processingPool) {
						logger.error("@postUpdateProcessing spuid timed out: " + spu.getSPUID());
					}
				}
			}
//		}
//		else {
//			logger.error("POST UPDATE PROCESSING ABORTED. Update processing by the endpoint return an error: "+ret);
//		}

		long stop = Timings.getTime();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** POST PROCESSING SUBSCRIPTIONS END *** ");

		processingMutex.release();
	}

	public void endOfProcessing(SPU s) {
		logger.debug("@endOfProcessing  SPUID: " + s.getSPUID());

		synchronized (processingPool) {
			processingPool.remove(s);
			if (processingPool.isEmpty())
				processingPool.notify();
		}
	}

	public void exceptionOnProcessing(SPU s) {
		logger.error("@exceptionOnProcessing  SPUID: " + s.getSPUID());

		activeSpus.remove(s);

		synchronized (processingPool) {
			processingPool.remove(s);
			if (processingPool.isEmpty())
				processingPool.notify();
		}
	}

	public Response subscribe(InternalSubscribeRequest req) throws InterruptedException {
		processingMutex.acquire();

		SPUManagerBeans.subscribeRequest();

		// Set the SPU Manager as event handler
		String sparql = req.getSparql();
		String alias = req.getAlias();
		String defaultGraph = req.getDefaultGraphUri();
		String namedGraph = req.getNamedGraphUri();
		InternalSubscribeRequest wrappedRequest = new InternalSubscribeRequest(sparql, alias, defaultGraph, namedGraph,
				this, req.getCredentials());

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

	public Response unsubscribe(String sid, String gid) throws InterruptedException {
		return internalUnsubscribe(sid, gid, true);
	}

	public void killSubscription(String sid, String gid) throws InterruptedException {
		internalUnsubscribe(sid, gid, false);
	}

	private Response internalUnsubscribe(String sid, String gid, boolean dep) throws InterruptedException {
		processingMutex.acquire();

		try {
			Subscriber sub = Subscriptions.getSubscriber(sid);
			String spuid = sub.getSPU().getSPUID();

			if (Subscriptions.removeSubscriber(sub)) {
				// If it is the last handler: kill SPU
				synchronized (spus) {
					spus.get(spuid).finish();
					spus.get(spuid).interrupt();

					// Clear
					spus.remove(spuid);

					logger.info("@internalUnsubscribe active SPUs: " + spus.size());
					
					SPUManagerBeans.setActiveSPUs(spus.size());
				}
			}
		} catch (SEPANotExistsException e) {
			logger.warn("@internalUnsubscribe SID not found: " + sid);

			processingMutex.release();

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
