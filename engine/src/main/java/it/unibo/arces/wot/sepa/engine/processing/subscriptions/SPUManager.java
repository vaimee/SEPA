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
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
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
import it.unibo.arces.wot.sepa.engine.scheduling.InternalPreProcessedUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.timing.Timings;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.jena.query.QueryException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private final Logger logger = LogManager.getLogger();

	// SPUs processing pool
	private final HashSet<SPU> processingPool = new HashSet<SPU>();
	private Collection<SPU> activeSpus;
	// SPUID ==> SPU
	private final HashMap<String, SPU> spus = new HashMap<String, SPU>();

	private final Processor processor;

	public SPUManager(Processor processor) {
		this.processor = processor;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public Response update(InternalUpdateRequest update) throws QueryException {
		logger.debug("*** UPDATE PROCESSING BEGIN *** Total running SPUs: " + spus.size());
		try {
			// PRE-processing update request
			InternalPreProcessedUpdateRequest preRequest = processor.preProcessUpdate(update);

			// STOP processing?
			if (preRequest.preProcessingFailed()) {
				logger.error("*** UPDATE PRE-PROCESSING FAILED *** " + preRequest.getErrorResponse());
				return preRequest.getErrorResponse();
			}

			// PRE-UPDATE subscriptions processing (ENDPOINT not yet updated)
			preUpdateSubscriptionsProcessing(preRequest);

			// UPDATE the ENDPOINT
			Response ret = processor.updateEndpoint(preRequest);

			// STOP processing?
			if (ret.isError()) {
				logger.error("*** UPDATE ENDPOINT PROCESSING FAILED *** " + ret);
				return ret;
			}

			// POST-UPDATE subscriptions processing (ENDPOINT not yet updated)
			postUpdateSubscriptionsProcessing(ret);

			logger.debug("*** UPDATE PROCESSING END *** ");

			return ret;

		} catch (SEPAProcessingException | SEPASecurityException e) {
			logger.error("*** SUBSCRIPTION PROCESSING EXCEPTION *** " + e.getMessage());
			return new ErrorResponse(500, "update_processing_failed",
					"Update: " + update + " Message: " + e.getMessage());
		}
	}

	private void preUpdateSubscriptionsProcessing(InternalPreProcessedUpdateRequest update)
			throws SEPAProcessingException {
		logger.trace("*** PRE-PROCESSING SUBSCRIPTIONS BEGIN *** ");

		// Get active SPUs (e.g., LUTT filtering)
		long start = Timings.getTime();
		activeSpus = Subscriptions.filter(update);
		long stop = Timings.getTime();
		SPUManagerBeans.filteringTimings(start, stop);

		// Start processing
		start = Timings.getTime();

		synchronized (processingPool) {
			// Copy active SPU pool
			processingPool.clear();

			for (SPU spu : activeSpus) {
				processingPool.add(spu);
				spu.preUpdateProcessing(update);
			}

			logger.debug("*** PRE-PROCESSING UPDATE *** SPU processing pool size: " + processingPool.size());

			// Wait all SPUs to complete processing
			if (!processingPool.isEmpty()) {
				logger.debug(String.format("Wait (%d ms) for %d SPUs to complete processing...",
						SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size(), processingPool.size()));

				try {

					processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size());

				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}

			stop = Timings.getTime();

			SPUManagerBeans.preProcessingTimings(start, stop);

			logger.trace("*** PRE-PROCESSING SUBSCRIPTIONS END *** ");

			if (!processingPool.isEmpty()) {
				logger.error(
						"@preUpdateProcessing TIMEOUT on SPU processing. SPUs still running: " + processingPool.size());
				throw new SEPAProcessingException(
						"@preUpdateProcessing TIMEOUT on SPU processing. SPUs still running: " + processingPool.size());
			}
		}
	}

	private void postUpdateSubscriptionsProcessing(Response ret) throws SEPAProcessingException {
		logger.trace("*** POST-PROCESSING SUBSCRIPTIONS BEGIN *** ");

		long start = Timings.getTime();

		synchronized (processingPool) {
			processingPool.clear();

			for (SPU spu : activeSpus) {
				processingPool.add(spu);
				spu.postUpdateProcessing(ret);
			}

			logger.debug("*** POST-PROCESSING SUBSCRIPTIONS *** SPU processing pool size: " + processingPool.size());

			if (!processingPool.isEmpty()) {
				logger.debug(String.format("Wait (%d ms) for %d SPUs to complete processing...",
						SPUManagerBeans.getSPUProcessingTimeout(), processingPool.size()));

				try {

					processingPool.wait(SPUManagerBeans.getSPUProcessingTimeout() * processingPool.size());

				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}

			long stop = Timings.getTime();

			SPUManagerBeans.postProcessingTimings(start, stop);

			logger.trace("*** POST-PROCESSING SUBSCRIPTIONS END *** ");

			if (!processingPool.isEmpty()) {
				logger.error("@postUpdateProcessing timeout on SPU processing. SPUs still running: "
						+ processingPool.size());
				throw new SEPAProcessingException(
						"@postUpdateProcessing timeout on SPU processing. SPUs still running: "
								+ processingPool.size());

			}
		}
	}

	public void endOfProcessing(SPU s) {
		logger.trace("@endOfProcessing  SPUID: " + s.getSPUID());

		synchronized (processingPool) {
			processingPool.remove(s);
			if (processingPool.isEmpty())
				processingPool.notify();
		}
	}

	public Response subscribe(InternalSubscribeRequest req) throws InterruptedException {

		SPUManagerBeans.subscribeRequest();

		// Create or link to an existing SPU
		SPU spu;
		if (Subscriptions.contains(req)) {
			spu = Subscriptions.getSPU(req);
		} else {
			spu = Subscriptions.createSPU(req, this);

			// Initialize SPU
			Response init;
			try {
				init = spu.init();
			} catch (SEPASecurityException e) {
				logger.error(e.getMessage());
				if (logger.isTraceEnabled())
					e.printStackTrace();
				init = new ErrorResponse(401, "SEPASecurityException", e.getMessage());
			}

			if (init.isError()) {
				logger.error("@subscribe SPU initialization failed: " + init);
				if (req.getAlias() != null) {
					((ErrorResponse) init).setAlias(req.getAlias());
				}

				return init;
			}

			// Register request
			Subscriptions.register(req, spu);

			// Create new entry for handler
			spus.put(spu.getSPUID(), spu);

			// Start the SPU thread
			spu.setName(spu.getSPUID());
			spu.start();
		}

		Subscriber sub = Subscriptions.addSubscriber(req, spu);

		return new SubscribeResponse(sub.getSID(), req.getAlias(), sub.getSPU().getLastBindings());
	}

	public Response unsubscribe(String sid, String gid) throws InterruptedException {
		return internalUnsubscribe(sid, gid, true);
	}

	public void killSubscription(String sid, String gid) throws InterruptedException {
		internalUnsubscribe(sid, gid, false);
	}

	private Response internalUnsubscribe(String sid, String gid, boolean dep) throws InterruptedException {

		try {
			Subscriber sub = Subscriptions.getSubscriber(sid);
			String spuid = sub.getSPU().getSPUID();

			if (Subscriptions.removeSubscriber(sub)) {
				// If it is the last handler: kill SPU
//				spus.get(spuid).finish();
				spus.get(spuid).interrupt();

				// Clear
				spus.remove(spuid);

				logger.info("@internalUnsubscribe active SPUs: " + spus.size());

				SPUManagerBeans.setActiveSPUs(spus.size());
			}
		} catch (SEPANotExistsException e) {
			logger.warn("@internalUnsubscribe SID not found: " + sid);

			return new ErrorResponse(500, "sid_not_found", "Unregistering a not existing subscriber: " + sid);
		}

		if (dep)
			Dependability.onUnsubscribe(gid, sid);

		return new UnsubscribeResponse(sid);
	}

	@Override
	public void notifyEvent(Notification notify) throws SEPAProtocolException {
		logger.trace("@notifyEvent " + notify);

		String spuid = notify.getSpuid();

		if (spus.containsKey(spuid)) {
			Subscriptions.notifySubscribers(spuid, notify);
		}
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

	public Response processQuery(InternalSubscribeRequest subscribe) throws SEPASecurityException {
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
