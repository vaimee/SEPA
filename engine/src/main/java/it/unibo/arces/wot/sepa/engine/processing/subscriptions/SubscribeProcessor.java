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
import java.util.concurrent.Semaphore;

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
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class SubscribeProcessor implements SubscribeProcessorMBean {
	private final Logger logger = LogManager.getLogger();

	private final Subscriber subscriber;
	private final Unsubscriber unsubscriber;

	private SPARQL11Properties endpointProperties;
	private Semaphore endpointSemaphore;
	
	private SPUManager spuManager =  new SPUManager();
		
	public SubscribeProcessor(SPARQL11Properties endpointProperties, EngineProperties engineProperties,
							  Semaphore endpointSemaphore) {
		this.endpointProperties = endpointProperties;
		this.endpointSemaphore = endpointSemaphore;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SubscribeProcessorBeans.setSPUProcessingTimeout(engineProperties.getSPUProcessingTimeout());

		this.subscriber = new Subscriber(spuManager);
		this.unsubscriber = new Unsubscriber(spuManager);
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

	public Response subscribe(SubscribeRequest req, EventHandler handler) {
		logger.trace(req.toString());

		SubscribeProcessorBeans.subscribeRequest();

		// TODO: choose different kinds of SPU based on subscribe request
		SPU spu = null;
		try {
			spu = new SPUNaive(req, handler, endpointProperties, endpointSemaphore, spuManager);
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
			try {
				subscriber.activate(spu);
			} catch (InterruptedException e) {
				return new ErrorResponse(req.getToken(),500,"Failed activating SPU");
			}
		}

		return init;
	}

	public Response unsubscribe(UnsubscribeRequest req) {
		logger.trace(req);

		SubscribeProcessorBeans.unsubscribeRequest();

		String spuid = req.getSubscribeUUID();

		if (!spuManager.isValidSpuId(spuid))
			return new ErrorResponse(req.getToken(), 404, "SPUID not found: " + spuid);

		try {
			unsubscriber.deactivate(spuid);
		} catch (InterruptedException e) {
			return new ErrorResponse(req.getToken(), 500, "Failed to unsubscribe: " + spuid);
		}

		return new UnsubscribeResponse(req.getToken(), spuid);
	}

	public void process(UpdateResponse update) {
		logger.debug("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
		Instant start = Instant.now();

		logger.debug("Activate SPUs (Total: " + spuManager.size() + ")");

		spuManager.startProcessing(update);

		// Wait all SPUs completing processing (or timeout)
		spuManager.waitEndOfProcessing();

		Instant stop = Instant.now();
		SubscribeProcessorBeans.timings(start, stop);

		logger.debug("*** PROCESSING SUBSCRIPTIONS END *** ");
	}

	@Override
	public long getRequests() {
		return SubscribeProcessorBeans.getRequests();
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
	public void setKeepalive(int t) {
		SubscribeProcessorBeans.setKeepalive(t);
	}

	@Override
	public int getKeepalive() {
		return SubscribeProcessorBeans.getKeepalive();
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
}
