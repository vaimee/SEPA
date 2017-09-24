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
import java.net.URISyntaxException;
import java.util.Observable;
//import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

import it.unibo.arces.wot.sepa.engine.core.EventHandler;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public abstract class SPU extends Observable implements Runnable {
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
	public abstract Notification processInternal(UpdateResponse update);

	public SPU(SubscribeRequest subscribe, SPARQL11Properties properties, EventHandler eventHandler)
			throws IllegalArgumentException, URISyntaxException {
		if (eventHandler == null)
			throw new IllegalArgumentException("Subscribe event handler is null");

		uuid = prefix + UUID.randomUUID().toString();
		logger = LogManager.getLogger("SPU" + uuid);

		request = subscribe;
		handler = eventHandler;

		queryProcessor = new QueryProcessor(properties);

		running = true;
	}

	public BindingsResults getFirstResults() {
		return firstResults;
	}

	public boolean isRunning() {
		return running;
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
		logger.debug("Send ping "+getUUID());
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
				Notification notify = processInternal(updateResponse);

				// Notify SPU manager
				logger.debug("Notify SPU manager. Running: " + running);
				setChanged();
				notifyObservers(true);

				// Notify
				try {
					if (notify != null)
						handler.notifyEvent(notify);
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
			
			// Terminated by an unsubscribe request
			if (!running) {
				logger.debug("*TERMINATED*");
				setChanged();
				notifyObservers(false);
				return;
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
}
