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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jena.query.QueryException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.QueryProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.UpdateProcessorBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.subscriptions.SPUManager;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalPreProcessedUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalQueryRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class Processor implements ProcessorMBean {
	// Processor threads
	private final UpdateProcessingThread updateProcessingThread;
	private final SubscribeProcessingThread subscribeProcessingThread;
	private final UnsubscribeProcessingThread unsubscribeProcessingThread;
	private final QueryProcessingThread queryProcessingThread;
	
	// SPARQL Processors
	private final QueryProcessor queryProcessor;
	private final UpdateProcessor updateProcessor;
	
	// SPU manager
	private final SPUManager spuManager;
	
	// Scheduler queue
	private final Scheduler scheduler;
	
	// Running flag
	private final AtomicBoolean running = new AtomicBoolean(true);
	
	public Processor(SPARQL11Properties endpointProperties, EngineProperties properties,
			Scheduler scheduler) throws IllegalArgumentException, SEPAProtocolException {		
		
		this.scheduler = scheduler;
		
		// Processors
		//queryProcessor = new QueryProcessor(endpointProperties,endpointSemaphore);
		queryProcessor = new QueryProcessor(endpointProperties);
		updateProcessor = new UpdateProcessor(endpointProperties);
		
		// SPU Manager
		spuManager = new SPUManager(this);
		
		// Subscribe/Unsubscribe processing
		subscribeProcessingThread = new SubscribeProcessingThread(this);
		unsubscribeProcessingThread = new UnsubscribeProcessingThread(this);
		
		// Update processor
		updateProcessingThread = new UpdateProcessingThread(this);
		
		// Query processing
		queryProcessingThread = new QueryProcessingThread(this);
		
		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);		
		ProcessorBeans.setEndpoint(endpointProperties);		
		QueryProcessorBeans.setTimeout(properties.getQueryTimeout());		
		UpdateProcessorBeans.setTimeout(properties.getUpdateTimeout());
		UpdateProcessorBeans.setReilable(properties.isUpdateReliable());
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
//	public QueryProcessor getQueryProcessor() {
//		return queryProcessor;
//	}
//	
//	public UpdateProcessor getUpdateProcessor() {
//		return updateProcessor;
//	}

	public void start() {
		running.set(true);
		queryProcessingThread.start();
		subscribeProcessingThread.start();
		unsubscribeProcessingThread.start();
		updateProcessingThread.start();
	}

	public void interrupt() {
		running.set(false);
		queryProcessingThread.interrupt();
		unsubscribeProcessingThread.interrupt();
		subscribeProcessingThread.interrupt();
		updateProcessingThread.interrupt();
	}
	
	public Response processSubscribe(InternalSubscribeRequest request) throws InterruptedException {
		return spuManager.subscribe(request);
	}
	public void killSubscription(String sid, String gid) throws InterruptedException {
		spuManager.killSubscription(sid, gid);
	}

	public Response unsubscribe(String sid, String gid) throws InterruptedException {
		return spuManager.unsubscribe(sid, gid);
	}
	
	public boolean isUpdateReliable() {
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

	public ScheduledRequest waitQueryRequest() throws InterruptedException {
		return scheduler.waitQueryRequest();
	}

	public void addResponse(int token, Response ret) {
		scheduler.addResponse(token, ret);		
	}

	public ScheduledRequest waitSubscribeRequest() throws InterruptedException {
		return scheduler.waitSubscribeRequest();
	}

	public ScheduledRequest waitUpdateRequest() throws InterruptedException {
		return scheduler.waitUpdateRequest();
	}

	public InternalPreProcessedUpdateRequest preProcessUpdate(InternalUpdateRequest update) throws QueryException {
		return updateProcessor.preProcess(update);
	}

	public Response updateEndpoint(InternalUpdateRequest preRequest) throws SEPASecurityException {
		return updateProcessor.process(preRequest);
	}

	public Response processUpdate(InternalUpdateRequest update) throws QueryException {
		return spuManager.update(update);
	}

	public ScheduledRequest waitUnsubscribeRequest() throws InterruptedException {
		return scheduler.waitUnsubscribeRequest();
	}

	public Response processQuery(InternalQueryRequest query) throws SEPASecurityException {
		return queryProcessor.process(query);
	}
}
