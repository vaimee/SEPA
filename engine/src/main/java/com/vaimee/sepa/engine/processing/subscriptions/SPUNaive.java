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

package com.vaimee.sepa.engine.processing.subscriptions;

import com.vaimee.sepa.api.commons.exceptions.SEPAProcessingException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.api.commons.response.QueryResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.response.SubscribeResponse;
import com.vaimee.sepa.api.commons.response.UpdateResponse;
import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.engine.scheduling.InternalSubscribeRequest;
import com.vaimee.sepa.engine.scheduling.InternalUpdateRequest;
import com.vaimee.sepa.logging.Logging;

import java.io.IOException;
import java.util.UUID;

class SPUNaive extends SPU {
	public SPUNaive(InternalSubscribeRequest subscribe, SPUManager manager) throws SEPAProtocolException {
		super(subscribe, manager);

		this.spuid = "sepa://spu/naive/" + UUID.randomUUID();

		Logging.debug("SPU: " + this.getSPUID() + " request: " + subscribe);
	}

	@Override
	public Response init() throws SEPASecurityException, IOException {
		Logging.log("spu","@init");

		// Process the SPARQL query
		Response ret = manager.processQuery(subscribe);

		if (ret.isError()) {
			Logging.error("Not initialized");
			return ret;
		}

		lastBindings = ((QueryResponse) ret).getBindingsResults();

		Logging.trace("First results: " + lastBindings.toString());

		return new SubscribeResponse(getSPUID(), subscribe.getAlias(), lastBindings);
	}

	@Override
	public void preUpdateInternalProcessing(InternalUpdateRequest req) throws SEPAProcessingException {
		Logging.log("spu","@preUpdateInternalProcessing");
	}

	@Override
	public Notification postUpdateInternalProcessing(UpdateResponse res) throws SEPAProcessingException {
		Logging.log("spu","@postUpdateInternalProcessing");
		
		Response ret = null;

		// Query the SPARQL processing service
		try {
			Logging.log("spu","Query endpoint");
			ret = manager.processQuery(subscribe);
		} catch (SEPASecurityException | IOException e) {
			if (Logging.isTraceEnabled()) e.printStackTrace();
			Logging.log("spu","Exception on query procesing "+e.getMessage());
			throw new SEPAProcessingException("postUpdateInternalProcessing exception "+e.getMessage());
		}

		if (ret.isError()) {
			Logging.log("spu","SEPAProcessingException "+ret);
			throw new SEPAProcessingException("postUpdateInternalProcessing exception "+ret.toString());
		}

		// Current and previous bindings
		BindingsResults results = ((QueryResponse) ret).getBindingsResults();
		BindingsResults currentBindings = new BindingsResults(results);

		// Initialize the results with the current bindings
		BindingsResults added = new BindingsResults(results.getVariables(), null);
		BindingsResults removed = new BindingsResults(results.getVariables(), null);

		// Create empty bindings if null
		if (lastBindings == null)
			lastBindings = new BindingsResults(null, null);

		Logging.trace("Current bindings: " + currentBindings);
		Logging.trace("Last bindings: " + lastBindings);

		// Find removed bindings
		long start = System.nanoTime();
		for (Bindings solution : lastBindings.getBindings()) {
			if (!results.contains(solution) && !solution.isEmpty())
				removed.add(solution);
			else
				results.remove(solution);
		}
		long stop = System.nanoTime();
		Logging.trace("Removed bindings: " + removed + " found in " + (stop - start) + " ns");

		// Find added bindings
		start = System.nanoTime();
		for (Bindings solution : results.getBindings()) {
			if (!lastBindings.contains(solution) && !solution.isEmpty())
				added.add(solution);
		}
		stop = System.nanoTime();
		Logging.trace("Added bindings: " + added + " found in " + (stop - start) + " ns");

		// Update the last bindings with the current ones
		lastBindings = currentBindings;
//		BindingsResults(null, null);
//		for (Bindings b : currentBindings.getBindings()) lastBindings.add(b);

		// Send notification (or end processing indication)
		if (!added.isEmpty() || !removed.isEmpty()) {
			Logging.log("spu","Send notification");
			return new Notification(getSPUID(), new ARBindingsResults(added, removed));
		}

		Logging.log("spu","Nothing to be notified");
		return null;
	}
}
