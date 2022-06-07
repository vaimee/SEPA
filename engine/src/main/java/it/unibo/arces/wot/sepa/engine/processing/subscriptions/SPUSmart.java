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

package it.unibo.arces.wot.sepa.engine.processing.subscriptions;


import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.lutt.FakeLUTT;
import it.unibo.arces.wot.sepa.engine.processing.lutt.LUTT;
import it.unibo.arces.wot.sepa.engine.processing.lutt.QueryLUTTextraction;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalSubscribeRequest;
import it.unibo.arces.wot.sepa.engine.scheduling.InternalUpdateRequest;
import it.unibo.arces.wot.sepa.logging.Logging;

import java.io.IOException;
import java.util.UUID;



//For now is the same to SPUNaive
/*
    The SPUNaive keep always the last bindings in memory
    The SPUSmart will keep it just if there will not too much (<maxBindings)
    otherwise, it will query the dataset before and after the update.
    SPUSmart is needed if you want to use LUTT, but it can be used without LUTT too
*/

class SPUSmart extends SPU {
	private static final int maxBindings = 10000;
	protected LUTT lutt;
	
	public SPUSmart(InternalSubscribeRequest subscribe, SPUManager manager) throws SEPAProtocolException {
		super(subscribe, manager);

		this.spuid = "sepa://spu/naive/" + UUID.randomUUID();

		Logging.logger.debug("SPUSmart: " +"; request: " + subscribe);
		if(EngineProperties.getIstance().isLUTTEnabled()) {
			this.lutt= QueryLUTTextraction.exstract(subscribe.getSparql());
		}else {
			this.lutt=new FakeLUTT();
		}
	}

	@Override
	public Response init() throws SEPASecurityException, IOException {
		Logging.logger.info("SPUSmart: "+getSPUID()+" @init");
		// Process the SPARQL query
		Response ret = manager.processQuery(subscribe);

		if (ret.isError()) {
			Logging.logger.error("SPUSmart Not initialized");
			return ret;
		}

		lastBindings = ((QueryResponse) ret).getBindingsResults();

		Logging.logger.trace("SPUSmart: "+getSPUID()+"; First results: " + lastBindings.toString());
		
		if(lastBindings.size()>maxBindings) {
			lastBindings=null;
		}
		
		return new SubscribeResponse(getSPUID(), subscribe.getAlias(), lastBindings);
	}

	@Override
	public void preUpdateInternalProcessing(InternalUpdateRequest req) throws SEPAProcessingException {
		Logging.logger.debug("SPUSmart: "+getSPUID()+"; @preUpdateInternalProcessing");
		
		if(lastBindings==null) {
			try {
				Logging.logger.trace("SPUSmart: "+getSPUID()+"; Query endpoint");
				Response ret = manager.processQuery(subscribe);
				if (ret.isError()) {
					Logging.logger.error("SPUSmart: "+getSPUID()+"; SEPAProcessingException "+ret);
					throw new SEPAProcessingException("preUpdateInternalProcessing exception "+ret.toString());
				}
				lastBindings= ((QueryResponse) ret).getBindingsResults();
			} catch (SEPASecurityException | IOException e) {
				//if (logger.isTraceEnabled()) e.printStackTrace();
				Logging.logger.error("SPUSmart: "+getSPUID()+"; SEPASecurityException "+e.getMessage());
				throw new SEPAProcessingException("preUpdateInternalProcessing exception "+e.getMessage());
			}
		}
	}

	@Override
	public Notification postUpdateInternalProcessing(UpdateResponse res) throws SEPAProcessingException {
		Logging.logger.debug("SPUSmart: "+getSPUID()+"; @postUpdateInternalProcessing");
		Response ret = null;

		// Query the SPARQL processing service
		try {
			Logging.logger.trace("SPUSmart: "+getSPUID()+"; Query endpoint");
			ret = manager.processQuery(subscribe);
		} catch (SEPASecurityException | IOException e) {
			//if (logger.isTraceEnabled()) e.printStackTrace();

			Logging.logger.error("SPUSmart: "+getSPUID()+"; SEPASecurityException "+e.getMessage());
			throw new SEPAProcessingException("postUpdateInternalProcessing exception "+e.getMessage());
		}

		if (ret.isError()) {
			Logging.logger.error("SPUSmart: "+getSPUID()+"; SEPAProcessingException "+ret);
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

		Logging.logger.trace("SPUSmart: "+getSPUID()+"; Current bindings: "+currentBindings);
		Logging.logger.trace("SPUSmart: "+getSPUID()+"; Last bindings: "+lastBindings);
		

		// Find removed bindings
		long start = System.nanoTime();
		for (Bindings solution : lastBindings.getBindings()) {
			if (!results.contains(solution) && !solution.isEmpty())
				removed.add(solution);
			else
				results.remove(solution);
		}
		long stop = System.nanoTime();
		Logging.logger.trace("SPUSmart: "+getSPUID()+"; Removed bindings: "+removed + " found in " + (stop - start) + " ns");
		// Find added bindings
		start = System.nanoTime();
		for (Bindings solution : results.getBindings()) {
			if (!lastBindings.contains(solution) && !solution.isEmpty())
				added.add(solution);
		}
		stop = System.nanoTime();
		Logging.logger.trace("SPUSmart: "+getSPUID()+"; Added bindings: " + added + " found in " + (stop - start) + " ns");
		

		// Update the last bindings with the current ones
		if(currentBindings.size()>maxBindings) {
			lastBindings=null;
		}else {
			lastBindings = currentBindings;
		}

		// Send notification (or end processing indication)
		if (!added.isEmpty() || !removed.isEmpty()) {
			Logging.logger.debug("SPUSmart: "+getSPUID()+"; Send notification");
			return new Notification(getSPUID(), new ARBindingsResults(added, removed));
		}

		Logging.logger.debug("SPUSmart: "+getSPUID()+"; Nothing to be notified");
		return null;
	}
}
