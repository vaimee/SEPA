/* This class implements a Semantic Processing Unit (SPU) of the Semantic Event Processing Architecture (SEPA) Engine
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
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

//public abstract class SPU extends Observable implements Runnable {
public abstract class SPU implements Runnable {
	private final Logger logger;

	// The URI of the subscription (i.e., sepa://spuid/UUID)
	private String uuid = null;
	private String prefix = "sepa://spuid/";

	// Update queue
	protected ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();

	// protected SPARQL11Protocol endpoint = null;
	protected QueryProcessor queryProcessor;

	protected SubscribeRequest request;
	protected EventHandler handler;

	// Thread loop
	private boolean running = true;

	// Query first results
	protected BindingsResults firstResults = null;

	// To be implemented by every specific SPU implementation
	public abstract boolean init();
	public abstract Response processInternal(UpdateResponse update,int timeout);
	
	//Notification result
	private Response notify;
	
	// List of processing SPU
	private HashSet<SPU> queue;
	
	public SPU(SubscribeRequest subscribe, SPARQL11Properties properties, EventHandler eventHandler,
			Semaphore endpointSemaphore, HashSet<SPU> queue) throws SEPAProtocolException {
		if (eventHandler == null)
			throw new SEPAProtocolException(new IllegalArgumentException("Subscribe event handler is null"));
		if (queue == null)
			throw new SEPAProtocolException(new IllegalArgumentException("SPU processing queue is null"));
		
		this.queue = queue;
		
		uuid = prefix + UUID.randomUUID().toString();
		logger = LogManager.getLogger("SPU" + uuid);

		request = subscribe;
		handler = eventHandler;

		queryProcessor = new QueryProcessor(properties, endpointSemaphore);

		running = true;
	}

	public BindingsResults getFirstResults() {
		return firstResults;
	}

	public void terminate() {
		synchronized (updateQueue) {
			running = false;
			updateQueue.notify();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!obj.getClass().equals(SPU.class))
			return false;
		return ((SPU) obj).getUUID().equals(getUUID());
	}

	public String getUUID() {
		return uuid;
	}

	public void process(UpdateResponse res) {
		synchronized (updateQueue) {
			updateQueue.offer(res);
			updateQueue.notify();
		}
	}

	public void ping() throws IOException {
		handler.sendPing(new Ping(getUUID()));
	}

	@Override
	public void run() {
		while (running) {
			// Poll the request from the queue
			UpdateResponse updateResponse;
			while ((updateResponse = updateQueue.poll()) != null && running) {
				// Processing update
				logger.debug("* PROCESSING *");

				//Asynchronous processing and waiting for result
				notify = processInternal(updateResponse,SPUManagerBeans.getSPUProcessingTimeout());
				
				// Notify event handler
				if (handler != null) { 
					if (notify.isNotification())
						try {
							handler.notifyEvent((Notification)notify);
						} catch (IOException e) {
							logger.error("Failed to notify "+notify);
						}
					else logger.debug("Not a notification: "+notify);
				}
				else logger.error("Handler is null");
				
				// Notify SPU manager
				logger.debug("Notify SPU manager. Running: " + running);
				synchronized (queue) {
					queue.remove(this);
					logger.debug("SPUs left: " + queue.size());
					queue.notify();
				}
			}

			// Wait next request...
			if (running)
				synchronized (updateQueue) {
					try {
						updateQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
		}
	}
}
