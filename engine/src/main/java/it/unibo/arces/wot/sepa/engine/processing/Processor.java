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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPUManager;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerQueue;

public class Processor implements ProcessorMBean {
	// Processor threads
	private final UpdateProcessingThread updateProcessingThread;
	private final SubscribeProcessingThread subscribeProcessingThread;
	private final QueryProcessingThread queryProcessingThread;
	
	// SPARQL Processors
	private final QueryProcessor queryProcessor;
	private final UpdateProcessor updateProcessor;
	
	// SPU manager
	private final SPUManager spuManager;

	// Concurrent endpoint limit
	private Semaphore endpointSemaphore = null;
	
	// Scheduler queue
	private final SchedulerQueue queue;
	
	// Running flag
	private final AtomicBoolean running = new AtomicBoolean(true);
	
	public Processor(SPARQL11Properties endpointProperties, EngineProperties properties,
			SchedulerQueue queue) throws IllegalArgumentException, SEPAProtocolException {		
		
		// Number of maximum concurrent requests (supported by the endpoint)
		int max = properties.getMaxConcurrentRequests();
		// TODO: extending at run-time the semaphore max
		if (max > 0) endpointSemaphore = new Semaphore(max, true);

		this.queue = queue;
		
		// Processors
		queryProcessor = new QueryProcessor(endpointProperties,endpointSemaphore);
		updateProcessor = new UpdateProcessor(endpointProperties,endpointSemaphore);
		
		// SPU Manager
		spuManager = new SPUManager(this);
		
		// Subscribe/Unsubscribe processing
		subscribeProcessingThread = new SubscribeProcessingThread(this);

		// Update processor
		updateProcessingThread = new UpdateProcessingThread(this);
		
		// Query processing
		queryProcessingThread = new QueryProcessingThread(this);
		
		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		
		ProcessorBeans.setEndpoint(endpointProperties, max);
		
		QueryProcessorBeans.setTimeout(properties.getQueryTimeout());
		
		UpdateProcessorBeans.setTimeout(properties.getUpdateTimeout());
		UpdateProcessorBeans.setReilable(properties.isUpdateReliable());
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
	public SchedulerQueue getSchedulerQueue() {
		return queue;
	}
	
	public SPUManager getSPUManager() {
		return spuManager;
	}
	
	public QueryProcessor getQueryProcessor() {
		return queryProcessor;
	}
	
	public UpdateProcessor getUpdateProcessor() {
		return updateProcessor;
	}

	public void start() {
		running.set(true);
		queryProcessingThread.start();
		subscribeProcessingThread.start();
		updateProcessingThread.start();
	}

	public void interrupt() {
		running.set(false);
		queryProcessingThread.interrupt();
		subscribeProcessingThread.interrupt();
		updateProcessingThread.interrupt();
	}
	
	public boolean isUpdateReilable() {
		return UpdateProcessorBeans.getReilable();
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
