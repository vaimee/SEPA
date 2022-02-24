/* 
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
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
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
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequestWithQuads;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.logging.Timings;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * The SEPA core class for subscriptions management. SpuManager is a monitor
 * class. It takes care of the SPU collection and it encapsulates filtering
 * algorithms based on the internal structure.
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.9.12
 */
public class SPUManager implements SPUManagerMBean, EventHandler {
	// SPUs processing pool
	private Collection<SPU> activeSpus = new HashSet<>();
	private Collection<SPU> processingPool = new HashSet<>();

	private final Processor processor;

	public SPUManager(Processor processor) {
		this.processor = processor;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public void abortSubscriptionsProcessing() {
		synchronized (activeSpus) {
			for (SPU spu : activeSpus)
				spu.abortProcessing();
		}
	}

	public boolean doUpdateARQuadsExtraction(InternalUpdateRequest update) {
		activeSpus = Subscriptions.filterOnGraphs(update);
		return !activeSpus.isEmpty();
	}

	public void subscriptionsProcessingPreUpdate(InternalUpdateRequest update) {
		if (update.getClass().equals(InternalUpdateRequestWithQuads.class))
			activeSpus = Subscriptions.filterOnQuads(activeSpus, (InternalUpdateRequestWithQuads) update);

		Logging.logger.log(Logging.getLevel("SPUManager"), "*** subscriptionsProcessingPreUpdate ***");

		// Start processing
		long start = Timings.getTime();

		// Copy active SPU pool
		synchronized (activeSpus) {
			Logging.logger.log(Logging.getLevel("SPUManager"),
					"*** subscriptionsProcessing *** create processing pool. Active SPUs: " + activeSpus.size());

			synchronized (processingPool) {
				processingPool.clear();
			}

			for (SPU spu : activeSpus) {
				processingPool.add(spu);
			}
		}

		synchronized (processingPool) {
			Logging.logger.log(Logging.getLevel("SPUManager"), "*** subscriptionsProcessing *** start processing");
			for (SPU spu : processingPool) {
				Logging.logger.log(Logging.getLevel("SPUManager"),
						"*** subscriptionsProcessing *** start SPU: " + spu.getSPUID());

				spu.preUpdateProcessing(update);
			}

			Logging.logger.log(Logging.getLevel("SPUManager"),
					"*** PRE-PROCESSING UPDATE *** SPU processing pool size: " + processingPool.size());

			// Wait all SPUs to complete processing
			while (!processingPool.isEmpty()) {
				Logging.logger.log(Logging.getLevel("SPUManager"),
						String.format("Wait (%d ms) for %d SPUs to complete processing...",
								SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size(),
								processingPool.size()));

				try {
					processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size());
				} catch (InterruptedException e) {
					Logging.logger.error(e.getMessage());
				}
			}
		}

		long stop = Timings.getTime();

		SPUManagerBeans.preProcessingTimings(start, stop);

		Logging.logger.log(Logging.getLevel("SPUManager"), "*** PRE-PROCESSING SUBSCRIPTIONS END *** ");
	}

	public void subscriptionsProcessingPostUpdate(Response updateResponse) {
		Logging.logger.log(Logging.getLevel("SPUManager"), "*** subscriptionsProcessingPostUpdate ***");

		// Start processing
		long start = Timings.getTime();

		// Copy active SPU pool
		synchronized (activeSpus) {
			Logging.logger.log(Logging.getLevel("SPUManager"),
					"*** subscriptionsProcessing *** create processing pool. Active SPUs: " + activeSpus.size());

			synchronized (processingPool) {
				processingPool.clear();
			}

			for (SPU spu : activeSpus) {
				processingPool.add(spu);
			}
		}

		synchronized (processingPool) {
			Logging.logger.log(Logging.getLevel("SPUManager"), "*** subscriptionsProcessing *** start processing");
			for (SPU spu : processingPool) {
				Logging.logger.log(Logging.getLevel("SPUManager"),
						"*** subscriptionsProcessing *** start SPU: " + spu.getSPUID());

				spu.postUpdateProcessing(updateResponse);
			}

			Logging.logger.log(Logging.getLevel("SPUManager"),
					"*** POST-PROCESSING UPDATE *** SPU processing pool size: " + processingPool.size());

			// Wait all SPUs to complete processing
			while (!processingPool.isEmpty()) {
				Logging.logger.log(Logging.getLevel("SPUManager"),
						String.format("Wait (%d ms) for %d SPUs to complete processing...",
								SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size(),
								processingPool.size()));

				try {
					processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size());
				} catch (InterruptedException e) {
					Logging.logger.error(e.getMessage());
				}
			}
		}

		long stop = Timings.getTime();

		SPUManagerBeans.postProcessingTimings(start, stop);

		Logging.logger.log(Logging.getLevel("SPUManager"), "*** POST-PROCESSING SUBSCRIPTIONS END *** ");
	}

	void endOfProcessing(SPU s) {
		Logging.logger.log(Logging.getLevel("SPUManager"), "@endOfProcessing  SPUID: " + s.getSPUID());

		synchronized (processingPool) {
			Logging.logger.log(Logging.getLevel("SPUManager"), "@endOfProcessing remove: " + s.getSPUID());
			processingPool.remove(s);
			Logging.logger.log(Logging.getLevel("SPUManager"), "@endOfProcessing notify processing pool");
			processingPool.notify();
		}
	}

	public Response subscribe(InternalSubscribeRequest req) {
		Logging.logger.log(Logging.getLevel("SPUManager"), "@subscribe");

		SPUManagerBeans.subscribeRequest();

		// Create or link to an existing SPU
		SPU spu;
		if (Subscriptions.containsSubscribe(req)) {
			spu = Subscriptions.getSPU(req);
		} else {
			spu = Subscriptions.createSPU(req, this);

			// Initialize SPU
			Response init;
			try {
				Logging.logger.log(Logging.getLevel("SPUManager"), "init SPU");
				init = spu.init();
			} catch (SEPASecurityException | IOException e) {
				Logging.logger.error(e.getMessage());
				if (Logging.logger.isTraceEnabled())
					e.printStackTrace();
				init = new ErrorResponse(401, "SEPASecurityException", e.getMessage());
			}

			if (init.isError()) {
				Logging.logger.error("@subscribe SPU initialization failed: " + init);
				if (req.getAlias() != null) {
					((ErrorResponse) init).setAlias(req.getAlias());
				}

				return init;
			}

			// Register request
			Logging.logger.log(Logging.getLevel("SPUManager"), "Register SPU");
			Subscriptions.registerSubscribe(req, spu);

			// Start the SPU thread
			spu.setName(spu.getSPUID());
			Logging.logger.log(Logging.getLevel("SPUManager"), "Start SPU");
			spu.start();
		}

		Subscriber sub = Subscriptions.addSubscriber(req, spu);

		return new SubscribeResponse(sub.getSID(), req.getAlias(), sub.getSPU().getLastBindings());
	}

//	public Response unsubscribe(String sid, String gid) {
//		Logging.logger.log(Level.getLevel("SPUManager"), "@unsubscribe " + sid + " " + gid);
//		return internalUnsubscribe(sid, gid, true);
//	}

//	public void killSubscription(String sid, String gid) {
//		Logging.logger.log(Level.getLevel("SPUManager"), "@killSubscription " + sid + " " + gid);
//		internalUnsubscribe(sid, gid, false);
//	}

	public Response unsubscribe(String sid, String gid) {
//	private Response internalUnsubscribe(String sid, String gid, boolean dep) {
		Logging.logger.log(Logging.getLevel("SPUManager"), "@internalUnsubscribe " + sid + " " + gid);// + " " + dep);

		try {
//			Subscriber sub = Subscriptions.getSubscriber(sid);
//
//			endOfProcessing(sub.getSPU());
//
//			synchronized (activeSpus) {
//				activeSpus.remove(sub.getSPU());
//			}

			Subscriptions.removeSubscriber(sid);

		} catch (SEPANotExistsException e) {
			Logging.logger.warn("@internalUnsubscribe SID not found: " + sid);
			return new ErrorResponse(500, "sid_not_found", "Unregistering a not existing subscriber: " + sid);
		}

		// if (dep)
		Dependability.onUnsubscribe(gid, sid);

		return new UnsubscribeResponse(sid);
	}

	@Override
	public void notifyEvent(Notification notify) {
		Logging.logger.log(Logging.getLevel("SPUManager"), "@notifyEvent");

		if (notify == null) {
			Logging.logger.warn("Nothing to be notified");
			return;
		}

		Subscriptions.notifySubscribers(notify);
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
	public void reset() {
		SPUManagerBeans.reset();
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
		SPUManagerBeans.setSPUProcessingTimeout(t);
		;
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

	Response processQuery(InternalSubscribeRequest subscribe) throws SEPASecurityException, IOException {
		return processor.processQuery(subscribe);
	}

	@Override
	public long getPreProcessingRequests() {
		return SPUManagerBeans.getPostProcessingUpdateRequests();
	}

	@Override
	public long getPostProcessingRequests() {
		return SPUManagerBeans.getPostProcessingUpdateRequests();
	}

	@Override
	public float getPreProcessing_SPUs_time() {
		return SPUManagerBeans.getPreProcessing_SPUs_time();
	}

	@Override
	public float getPreProcessing_SPUs_time_min() {
		return SPUManagerBeans.getPreProcessing_SPUs_time_min();
	}

	@Override
	public float getPreProcessing_SPUs_time_max() {
		return SPUManagerBeans.getPreProcessing_SPUs_time_max();
	}

	@Override
	public float getPreProcessing_SPUs_time_average() {
		return SPUManagerBeans.getPreProcessing_SPUs_time_average();
	}

	@Override
	public float getPostProcessing_SPUs_time() {
		return SPUManagerBeans.getPostProcessing_SPUs_time();
	}

	@Override
	public float getPostProcessing_SPUs_time_min() {
		return SPUManagerBeans.getPostProcessing_SPUs_time_min();
	}

	@Override
	public float getPostProcessing_SPUs_time_max() {
		return SPUManagerBeans.getPostProcessing_SPUs_time_max();
	}

	@Override
	public float getPostProcessing_SPUs_time_average() {
		return SPUManagerBeans.getPostProcessing_SPUs_time_average();
	}

	@Override
	public long getPreProcessingExceptions() {
		return SPUManagerBeans.getPreProcessingExceptions();
	}

	@Override
	public long getPostProcessingExceptions() {
		return SPUManagerBeans.getPostProcessingExceptions();
	}

	@Override
	public long getNotifyExceptions() {
		return SPUManagerBeans.getNotifyExceptions();
	}
}
