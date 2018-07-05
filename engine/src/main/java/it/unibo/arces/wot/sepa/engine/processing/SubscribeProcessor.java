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

package it.unibo.arces.wot.sepa.engine.processing;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SubscribeProcessorBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.ISPU;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPUManager;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPUNaive;

class SubscribeProcessor implements SubscribeProcessorMBean, EventHandler {
	private final Logger logger = LogManager.getLogger();

	private SPARQL11Properties endpointProperties;
	private Semaphore endpointSemaphore;

	private SPUManager spuManager = new SPUManager();

	// REAL spuid => list of FAKE spuids
	private HashMap<String, ArrayList<String>> spuidPoll = new HashMap<String, ArrayList<String>>();

	// FAKE spuid ==> handler
	private HashMap<String, EventHandler> handlers = new HashMap<String, EventHandler>();

	// Handler ==> sequence number
	private HashMap<EventHandler, Integer> sequenceNumbers = new HashMap<EventHandler, Integer>();

	// FAKE spuid ==> REAL spuid
	private HashMap<String, String> fakeMap = new HashMap<String, String>();

	public SubscribeProcessor(SPARQL11Properties endpointProperties, EngineProperties engineProperties,
			Semaphore endpointSemaphore) {
		this.endpointProperties = endpointProperties;
		this.endpointSemaphore = endpointSemaphore;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SubscribeProcessorBeans.setSPUProcessingTimeout(engineProperties.getSPUProcessingTimeout());
	}

	public void start() {
		spuManager.start();
	}

	public void stop() {
		spuManager.stop();
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		logger.warn("Not implemented: " + response);
	}

	@Override
	public void notifyEvent(Notification notify) throws IOException {
		for (String spuid : spuidPoll.get(notify.getSpuid())) {
			EventHandler handler = handlers.get(spuid);

			handler.notifyEvent(new Notification(spuid, notify.getARBindingsResults(), sequenceNumbers.get(handler)));

			sequenceNumbers.put(handler, sequenceNumbers.get(handler) + 1);
		}
	}

	public void process(UpdateResponse update) {
		logger.debug("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
		Instant start = Instant.now();

		// Start subscription processing
		spuManager.startProcessing(update);

		// Wait all SPUs completing processing (or timeout)
		spuManager.waitEndOfProcessing();

		Instant stop = Instant.now();

		SubscribeProcessorBeans.timings(start, stop);

		logger.debug("*** PROCESSING SUBSCRIPTIONS END *** ");
	}
	
	public Response subscribe(SubscribeRequest req, EventHandler handler) {
		logger.trace(req.toString());

		SubscribeProcessorBeans.subscribeRequest();

		// Is SPU already available or do we need to create a new one?
		ISPU spu = spuManager.getSPU(req);
		if (spu == null) spu = createSPU(req);
		if (spu == null) return new ErrorResponse(req.getToken(), 500, "SPU creation failed: " + req.toString());
		
		// Generate a fake SPU id
		String spuid = spuManager.generateSpuid();

		// Register handler
		registerHandler(spu.getUUID(), spuid, handler);

		return new SubscribeResponse(req.getToken(), spuid, spu.getLastBindings());
	}
	
	public Response unsubscribe(UnsubscribeRequest req) {
		logger.trace(req);

		SubscribeProcessorBeans.unsubscribeRequest();

		String spuid = req.getSubscribeUUID();
		String masterSpuid = fakeMap.get(spuid);

		logger.debug("Master spuid: " + masterSpuid + " (" + spuid + ")");
		
		if (masterSpuid == null) return new ErrorResponse(req.getToken(), 404, "SPUID not found: " + spuid);
		if (!spuManager.isValidSpuId(masterSpuid)) return new ErrorResponse(req.getToken(), 404, "SPUID not found: " + masterSpuid);

		// Unregister handler
		unregisterHandler(masterSpuid, spuid);

		if (spuidPoll.get(masterSpuid).isEmpty()) {
			spuidPoll.remove(masterSpuid);

			// Deactivate SPU
			spuManager.deactivate(masterSpuid);
		}

		return new UnsubscribeResponse(req.getToken(), spuid);
	}
	
	// TODO: choose different kinds of SPU based on subscribe request
	private ISPU createSPU(SubscribeRequest req) {
		ISPU spu;

		try {
			spu = new SPUNaive(req, this, endpointProperties, endpointSemaphore, spuManager);
		} catch (SEPAProtocolException e) {
			logger.debug("SPU creation failed: " + e.getMessage());
			return null;
		}

		// Initialize SPU
		Response init = spu.init();
		if (init.isError()) {
			logger.error("SPU initialization failed");
			return null;
		}

		logger.debug("Add SPU to activation queue");

		// Request SPU activation
		if(!spuManager.activate(spu, req)) return null;

		return spu;
	}

	private void registerHandler(String masterSpuid, String spuid, EventHandler handler) {
		logger.debug("Register SPU handler: " + spuid);
		if (spuidPoll.get(masterSpuid) == null)
			spuidPoll.put(masterSpuid, new ArrayList<String>());

		spuidPoll.get(masterSpuid).add(spuid);
		handlers.put(spuid, handler);
		sequenceNumbers.put(handler, 1);
		fakeMap.put(spuid, masterSpuid);
	}
	
	private void unregisterHandler(String masterSpuid, String spuid) {
		fakeMap.remove(spuid);
		sequenceNumbers.remove(handlers.get(spuid));
		handlers.remove(spuid);
		spuidPoll.get(masterSpuid).remove(spuid);
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
