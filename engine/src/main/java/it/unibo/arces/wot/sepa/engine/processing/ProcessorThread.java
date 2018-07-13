/*  This class implements the processing of the requests coming form the scheduler
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerRequestResponseQueue;

import org.apache.logging.log4j.LogManager;

public class ProcessorThread extends Thread implements ProcessorThreadMBean {
	private final Logger logger = LogManager.getLogger();

	// Processors
	private final UpdateProcessingThread updateProcessor;
	private final QueryProcessor queryProcessor;
	private final SubscribeProcessor subscribeProcessor;

	// Broken SPU killer
	private final SpuKillerThread spuKiller;
	
	// Scheduler queue
	private SchedulerRequestResponseQueue schedulerQueue;

	// Concurrent endpoint limit
	private Semaphore endpointSemaphore = null;

	public ProcessorThread(SPARQL11Properties endpointProperties, EngineProperties properties,
			SchedulerRequestResponseQueue queue,BlockingQueue<String> killSpuids) throws IllegalArgumentException, SEPAProtocolException {
		if (queue == null) {
			logger.error("Queue is null");
			throw new IllegalArgumentException("Queue is null");
		}
		this.schedulerQueue = queue;

		// TODO: extending at run-time the semaphore max
		// Number of maximum concurrent requests (supported by the endpoint)
		int max = properties.getMaxConcurrentRequests();
		if (max > 0)
			endpointSemaphore = new Semaphore(max, true);

		// Query processor
		queryProcessor = new QueryProcessor(endpointProperties, endpointSemaphore);

		// SPU manager
		subscribeProcessor = new SubscribeProcessor(endpointProperties, properties, endpointSemaphore);

		// Update processor
		if (properties.isUpdateReliable())
			updateProcessor = new UpdateProcessingThread(new UpdateProcessor(endpointProperties, endpointSemaphore),
					subscribeProcessor, queue);
		else
			updateProcessor = new UpdateProcessingThread(new UpdateProcessor(endpointProperties, endpointSemaphore),
					subscribeProcessor, null);

		// SPU killer
		spuKiller = new SpuKillerThread(killSpuids,subscribeProcessor);
		
		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		ProcessorBeans.setEndpoint(endpointProperties, max);
		QueryProcessorBeans.setTimeout(properties.getQueryTimeout());
		UpdateProcessorBeans.setTimeout(properties.getUpdateTimeout());
		UpdateProcessorBeans.setReilable(properties.isUpdateReliable());
	}

	@Override
	public void run() {
		while (true) {
			// WAIT NEW REQUEST
			ScheduledRequest scheduledRequest;
			try {
				scheduledRequest = schedulerQueue.waitRequest();
			} catch (InterruptedException e1) {
				return;
			}

			Request request = scheduledRequest.getRequest();
			if (request.isUpdateRequest()) {
				logger.debug("Update request #" + request.getToken());
				logger.trace(request);

				// Update response QoS
				if (UpdateProcessorBeans.getReilable()) {
					updateProcessor.setSchedulerQueue(schedulerQueue);
				}
				else {
					updateProcessor.setSchedulerQueue(null);
					schedulerQueue.addResponse(new UpdateResponse(request.getToken(),"{\"Request scheduled for processing\"}"));
				}
				
				// Add a new UpdateRequest to be processed
				updateProcessor.process((UpdateRequest) request);
			} else if (request.isQueryRequest()) {
				logger.debug("Query request #" + request.getToken());
				logger.trace(request);

				Thread queryProcessing = new Thread() {
					public void run() {
						Response ret = queryProcessor.process((QueryRequest) request);
						schedulerQueue.addResponse(ret);
					}
				};
				queryProcessing.setName("SEPA-Query-Processing-Thread-" + request.getToken());
				queryProcessing.start();
			} else if (request.isSubscribeRequest()) {
				logger.debug("Subscribe request #" + request.getToken());
				logger.trace(request);

				Response ret = subscribeProcessor.subscribe((SubscribeRequest) request,
						(EventHandler) scheduledRequest.getHandler());

				schedulerQueue.addResponse(ret);
			} else if (request.isUnsubscribeRequest()) {
				logger.info("Unsubscribe request #" + request.getToken());
				logger.debug(request);

				Response ret = subscribeProcessor.unsubscribe((UnsubscribeRequest) request);

				schedulerQueue.addResponse(ret);
			}
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		subscribeProcessor.start();
		updateProcessor.start();
		spuKiller.start();
	}

	@Override
	public void interrupt() {
		super.interrupt();
		subscribeProcessor.stop();
		
		updateProcessor.finish();
		updateProcessor.interrupt();
		
		spuKiller.finish();
		spuKiller.interrupt();
	}

	@Override
	public String getEndpointHost() {
		return ProcessorBeans.getEndpointHost();
	}

	@Override
	public int getEndpointPort() {
		return ProcessorBeans.getEndpointPort();
	}

	@Override
	public String getEndpointQueryPath() {
		return ProcessorBeans.getEndpointQueryPath();
	}

	@Override
	public String getEndpointUpdatePath() {
		return ProcessorBeans.getEndpointUpdatePath();
	}

	@Override
	public String getEndpointUpdateMethod() {
		return ProcessorBeans.getEndpointUpdateMethod();
	}

	@Override
	public String getEndpointQueryMethod() {
		return ProcessorBeans.getEndpointQueryMethod();
	}

	@Override
	public int getMaxConcurrentRequests() {
		return ProcessorBeans.getMaxConcurrentRequests();
	}
}
