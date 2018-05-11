/* This class implements the manager of the Semantic Processing Units (SPUs) of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import it.unibo.arces.wot.sepa.engine.processing.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class SubscribeProcessor implements SPUManagerMBean {
	private final Logger logger = LogManager.getLogger("SubscribeProcessor");
	private final Subscriber subscriber;
	private final Unsubcriber unsubscriber;

	private SPARQL11Properties endpointProperties;


	private SPUManager spuManager =  new SPUManager();

	// Request queue
	private LinkedBlockingQueue<ISubscriptionProcUnit> subscribeQueue = new LinkedBlockingQueue<>();
	private LinkedBlockingQueue<String> unsubscribeQueue = new LinkedBlockingQueue<>();
	private ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();

	private Semaphore endpointSemaphore;

	// SPU Synchronization
	private SPUSync spuSync = new SPUSync();

	public SubscribeProcessor(SPARQL11Properties endpointProperties, EngineProperties engineProperties,
							  Semaphore endpointSemaphore, UpdateProcessingQueue updateProcessingQueue) {
		this.endpointProperties = endpointProperties;
		this.endpointSemaphore = endpointSemaphore;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SPUManagerBeans.setSPUProcessingTimeout(engineProperties.getSPUProcessingTimeout());

		this.subscriber = new Subscriber(subscribeQueue,spuManager);
		this.unsubscriber = new Unsubcriber(unsubscribeQueue,spuManager);

		// Main update processing thread
		Thread main = new Thread() {
			public void run() {
				while (true) {
					UpdateResponse update;
					while ((update = updateQueue.poll()) != null) {
						logger.info("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
						Instant start = Instant.now();

						logger.info("Activate SPUs (Total: " + spuManager.size() + ")");

						spuSync.startProcessing(spuManager.getAll());
							//TODO: filter algorithm
                        for (ISubscriptionProcUnit spu : spuManager.getAll())
                            spu.process(update);


						// Wait all SPUs completing processing (or timeout)
						spuSync.waitEndOfProcessing();

						Instant stop = Instant.now();
						SPUManagerBeans.timings(start, stop);

						// Notify processor of end of processing
						updateProcessingQueue.updateEOP(new SPUEndOfProcessing(!spuSync.isEmpty()));

						logger.info("*** PROCESSING SUBSCRIPTIONS END *** ");
					}
					synchronized (updateQueue) {
						try {
							updateQueue.wait();
						} catch (InterruptedException e) {
							return;
						}
					}
				}
			}
		};
		main.setName("SEPA SPU Manager");
		main.start();

	}

	public void start(){
		this.subscriber.start();
		this.unsubscriber.start();
	}

	public void stop(){
		this.subscriber.finish();
		this.unsubscriber.finish();

		this.subscriber.interrupt();
		this.unsubscriber.interrupt();
	}

	public Response subscribe(SubscribeRequest req, EventHandler handler) {
		logger.debug(req.toString());

		SPUManagerBeans.subscribeRequest();

		// TODO: choose different kinds of SPU based on subscribe request
		SPU spu = null;
		try {
			spu = new SPUNaive(req, handler, endpointProperties, endpointSemaphore, spuSync);
		} catch (SEPAProtocolException e) {
			logger.debug("SPU creation failed: " + e.getMessage());

			return new ErrorResponse(req.getToken(), 500, "SPU creation failed: " + req.toString());
		}

		logger.debug("SPU init");

		Response init = spu.init();

		if (init.isError()) {
			logger.debug("SPU initialization failed");
		} else {
			logger.debug("Add SPU to activation queue");
			subscribeQueue.offer(spu);
		}

		return init;
	}

	public Response unsubscribe(UnsubscribeRequest req) {
		logger.debug(req);

		SPUManagerBeans.unsubscribeRequest();

		String spuid = req.getSubscribeUUID();

		if (!spuManager.isValidSpuId(spuid))
			return new ErrorResponse(req.getToken(), 404, "SPUID not found: " + spuid);

		unsubscribeQueue.offer(spuid);

		return new UnsubscribeResponse(req.getToken(), spuid);
	}

	public void process(UpdateResponse update) {
		synchronized (updateQueue) {
			logger.debug("Add to update response queue: " + update);
			updateQueue.offer(update);
			updateQueue.notify();
		}
	}

	@Override
	public long getRequests() {
		return SPUManagerBeans.getRequests();
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
	public void setKeepalive(int t) {
		SPUManagerBeans.setKeepalive(t);
	}

	@Override
	public int getKeepalive() {
		return SPUManagerBeans.getKeepalive();
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
}
