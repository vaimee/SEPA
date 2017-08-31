/* This class implements a naive implementation of a SPU
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

import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;

public class SPUNaive extends SPU {
	private static final Logger logger = LogManager.getLogger("SPUNaive");

	private BindingsResults lastBindings = null;
	private Integer sequence = 0;
	private BindingsResults firstResults = null;
	
	public SPUNaive(ScheduledRequest subscribe, SPARQL11Protocol endpoint) {
		super(subscribe, endpoint);
	}
	
//	@Override 
//	public void run(){
//		ARBindingsResults bindings = new ARBindingsResults(lastBindings, null);
//		Notification notification = new Notification(getUUID(), bindings, sequence++);
//
//		if (subscribe.getEventHandler() != null)
//			try {
//				subscribe.getEventHandler().notifyEvent(notification);
//			} catch (IOException e) {
//				logger.error(e.getMessage());
//			}
//		
//		super.run();
//	}

	@Override
	public boolean init() {
		// Process the subscribe SPARQL query
		Response ret = queryProcessor.process((SubscribeRequest) subscribe.getRequest());
		

		if (ret.getClass().equals(ErrorResponse.class))
			return false;

		lastBindings = ((QueryResponse) ret).getBindingsResults();
		firstResults = new BindingsResults(lastBindings);
				
		return true;
	}

	@Override
	public synchronized void process(UpdateResponse update) {
		logger.debug("Start processing " + this.getUUID());

		// Query the SPARQL processing service
		Response ret = queryProcessor.process((QueryRequest) subscribe.getRequest());

		if (ret.getClass().equals(ErrorResponse.class))
			return;

		QueryResponse currentResults = (QueryResponse) ret;

		// Current and previous bindings
		BindingsResults currentBindings = currentResults.getBindingsResults();
		BindingsResults newBindings = new BindingsResults(currentBindings);

		// Initialize the results with the current bindings
		BindingsResults added = new BindingsResults(currentBindings.getVariables(), null);
		BindingsResults removed = new BindingsResults(currentBindings.getVariables(), null);

		// Create empty bindings if null
		if (lastBindings == null)
			lastBindings = new BindingsResults(null, null);

		// Find removed bindings
		for (Bindings solution : lastBindings.getBindings()) {
			if (!currentBindings.contains(solution) && !solution.isEmpty())
				removed.add(solution);
			else
				currentBindings.remove(solution);
		}

		// Find added bindings
		for (Bindings solution : currentBindings.getBindings()) {
			if (!lastBindings.contains(solution) && !solution.isEmpty())
				added.add(solution);
		}

		// Update the last bindings with the current ones
		lastBindings = new BindingsResults(newBindings);

		// Send notification (or end processing indication)
		if (!added.isEmpty() || !removed.isEmpty()) {
			ARBindingsResults bindings = new ARBindingsResults(added, removed);
			Notification notification = new Notification(getUUID(), bindings, sequence);

			if (subscribe.getEventHandler() != null && notification.toBeNotified())
				try {
					subscribe.getEventHandler().notifyEvent(notification);
					sequence++;
				} catch (IOException e) {
					logger.error(e.getMessage());
				}
		}

		// Notify SPU manager of the SPU end processing
		setChanged();
		notifyObservers(new Notification(getUUID(), null, -1));

		logger.debug(getUUID() + " End processing");
	}

	@Override
	public BindingsResults getFirstResults() {
		return firstResults;
	}
}
