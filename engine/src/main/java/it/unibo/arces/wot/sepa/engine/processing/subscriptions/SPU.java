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

package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public abstract class SPU extends Thread implements ISPU {
	protected final Logger logger = LogManager.getLogger();

	// SPU identifier
	protected String spuid;

	// Update queue
	private final LinkedBlockingQueue<InternalUpdateRequest> updateRequestQueue = new LinkedBlockingQueue<InternalUpdateRequest>();
	private final LinkedBlockingQueue<Response> updateResponseQueue = new LinkedBlockingQueue<Response>();

	protected final InternalSubscribeRequest subscribe;

	// Thread loop
	private final AtomicBoolean running = new AtomicBoolean(true);

	// Last bindings results
	protected BindingsResults lastBindings = null;

	// Notification result
	private Response notify;

	// List of processing SPU
	protected final SPUManager manager;

	public SPU(InternalSubscribeRequest subscribe, SPUManager manager) {
		this.manager = manager;
		this.subscribe = subscribe;
		this.spuid = "sepa://spu/" + UUID.randomUUID();
	}

	public InternalSubscribeRequest getSubscribe() {
		return subscribe;
	}

	public abstract Response postUpdateInternalProcessing();

	public abstract void preUpdateInternalProcessing(InternalUpdateRequest update);

	@Override
	public BindingsResults getLastBindings() {
		return lastBindings;
	}

	public final void finish() {
		running.set(false);
	}

	@Override
	public final boolean equals(Object obj) {
		return ((SPU) obj).subscribe.equals(subscribe);
	}

	@Override
	public final String getSPUID() {
		return spuid;
	}

	@Override
	public final int hashCode() {
		return subscribe.hashCode();
	}

	@Override
	public final void postProcessing(Response res) {
		try {
			updateResponseQueue.put(res);
		} catch (InterruptedException e) {

		}
	}

	@Override
	public final void preProcessing(InternalUpdateRequest res) {
		try {
			updateRequestQueue.put(res);
		} catch (InterruptedException e) {

		}
	}

	@Override
	public void run() {
		while (running.get()) {
			// Poll the request from the queue
			InternalUpdateRequest request;
			Response response;

			try {
				request = updateRequestQueue.take();
			} catch (InterruptedException e1) {
				return;
			}

			// Processing update
			logger.debug("* PRE PROCESSING *");

			// PRE processing
			preUpdateInternalProcessing(request);

			// Notify SPU manager
			logger.debug("Notify SPU manager. Running: " + running);
			manager.endOfProcessing(this);

			// Wait
			try {
				response = updateResponseQueue.take();
			} catch (InterruptedException e1) {
				return;
			}

			if (!response.isError()) {
				// POST processing and waiting for result
				notify = postUpdateInternalProcessing();

				// Notify event handler
				if (notify.isNotification())
					try {
						subscribe.getEventHandler().notifyEvent((Notification) notify);
					} catch (SEPAProtocolException e) {
						logger.error(e.getMessage());
					}
				else
					logger.debug("Not a notification: " + notify);
			}

			// Notify SPU manager
			logger.debug("Notify SPU manager. Running: " + running);
			manager.endOfProcessing(this);

		}
	}
}
