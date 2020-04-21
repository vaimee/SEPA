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
import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.engine.scheduling.InternalPreProcessedUpdateRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
 * @author Luca Roffia (luca.roffia@unibo.it)
 * @version 0.1
 */

public abstract class SPU extends Thread implements ISPU {

	// To be implemented by a specific SPU
	public abstract Notification postUpdateInternalProcessing(UpdateResponse ret) throws SEPAProcessingException, SEPASecurityException;

	public abstract void preUpdateInternalProcessing(InternalUpdateRequest update) throws SEPAProcessingException;

	protected final Logger logger = LogManager.getLogger();

	// SPU identifier
	protected String spuid;

	// Thread loop
	private final AtomicBoolean running = new AtomicBoolean(true);

	// Last bindings results
	protected BindingsResults lastBindings = null;
	
	// Request and response
	private final AtomicBoolean preProcessing = new AtomicBoolean();
	protected InternalUpdateRequest request;
	protected Response response;
	protected final InternalSubscribeRequest subscribe;
	
	protected final SPUManager manager;

	public SPU(InternalSubscribeRequest subscribe, SPUManager manager) {
		this.manager = manager;
		this.subscribe = subscribe;
		this.spuid = "sepa://spu/" + UUID.randomUUID();
	}

	public InternalSubscribeRequest getSubscribe() {
		return subscribe;
	}

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
	public final void postUpdateProcessing(Response res) {
		response = res;
		synchronized(preProcessing) {
			preProcessing.set(false);
			preProcessing.notify();
		}	
	}

	@Override
	public final void preUpdateProcessing(InternalPreProcessedUpdateRequest req) {
		request = req;
		synchronized(preProcessing) {
			preProcessing.set(true);
			preProcessing.notify();
		}
	}

	@Override
	public void run() {
		while (running.get()) {
			try {
				synchronized(preProcessing) {
					preProcessing.wait();
				}
			}
			catch(InterruptedException e) {
				logger.warn(e.getMessage());
				return;
			}
			
			try {
				if (preProcessing.get()) {
					// PRE processing
					logger.debug("* PRE PROCESSING *");
					preUpdateInternalProcessing(request);
				}
				else {
					// POST processing
					logger.debug("* POST PROCESSING *");
					Notification notify = postUpdateInternalProcessing((UpdateResponse) response);
					
					// NOTIFY event
					if (notify != null) manager.notifyEvent(notify);
				}
			}
			catch(SEPAProcessingException | SEPASecurityException | SEPAProtocolException e) {
				logger.error(e.getMessage());
				if (logger.isTraceEnabled()) e.printStackTrace();
			}
			
									
			// End of processing
			logger.trace("Notify SPU manager of EOP. Running: " + running);
			manager.endOfProcessing(this);
		}
	}
}
