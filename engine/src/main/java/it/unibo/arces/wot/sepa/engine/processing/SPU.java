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
import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public abstract class SPU extends Observable implements Runnable {
	private static final Logger logger = LogManager.getLogger("SPU");

	// The URI of the subscription (i.e., sepa://spuid/UUID)
	private String uuid = null;
	private String prefix = "sepa://spuid/";

	// Update queue
	protected ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();

	// Subscription
	protected QueryProcessor queryProcessor = null;
	protected ScheduledRequest subscribe = null;

	// Thread loop
	private boolean running = true;

	// To be implemented
	public abstract boolean init();
	public abstract void process(UpdateResponse update);
	public abstract BindingsResults getFirstResults();
	
	class SubscriptionProcessingInputData {
		public UpdateResponse update = null;
		public QueryProcessor queryProcessor = null;
		public SubscribeRequest subscribe = null;
	}

	public SPU(ScheduledRequest subscribe, SPARQL11Protocol endpoint) {
		this.uuid = prefix + UUID.randomUUID().toString();
		this.subscribe = subscribe;
		this.queryProcessor = new QueryProcessor(endpoint);
		this.running = true;
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

	public boolean sendPing() {
		try {
			subscribe.getEventHandler().sendPing(new Ping(getUUID()));
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public String getUUID() {
		return uuid;
	}

	public void subscriptionCheck(UpdateResponse res) {
		synchronized (updateQueue) {
			updateQueue.offer(res);
			updateQueue.notify();
		}

	}

	private UpdateResponse waitUpdate() {
		synchronized (updateQueue) {
			while (updateQueue.isEmpty()) {

				try {
					logger.debug(getUUID() + " Waiting new update response...");

					updateQueue.wait();

				} catch (InterruptedException e) {
					return null;
				}

				if (!running)
					return null;
			}

			return updateQueue.poll();
		}
	}

	@Override
	public void run() {
		// Main loop
		logger.debug(getUUID() + " Entering main loop...");
		while (running) {
			// Wait new update
			UpdateResponse update = waitUpdate();
			if (update == null)
				continue;

			// Processing
			process(update);
		}
		logger.debug(getUUID() + " terminated");
	}
}
