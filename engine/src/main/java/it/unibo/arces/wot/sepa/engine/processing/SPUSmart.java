/* This class implements a naive implementation of a SPU
 * 
 * Author: Fabio Viola (fabio.viola@unibo.it)

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

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
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
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.logging.log4j.LogManager;

public class SPUSmart extends SPU {
	private final Logger logger;

	private BindingsResults lastBindings = null;
	private Integer sequence = 0;
	private Response ret;
	// private List<TriplePath> lutt;
	private LUTT lutt = null;	
	private ContextTripleStore ctss;

	public SPUSmart(SubscribeRequest subscribe, EventHandler handler, SPARQL11Properties endpointProperties)
			throws IllegalArgumentException, URISyntaxException {
		super(subscribe, endpointProperties, handler);

		logger = LogManager.getLogger("SPUSmart_" + getUUID());
		logger.debug("SPU: " + this.getUUID() + " request: " + subscribe);
	}

	@Override
	public boolean init() throws ParseException {
		logger.debug("Process SPARQL query " + request);
		
		// manipulate query to extract triple patterns
		String query = request.getSPARQL();
		SPARQLAnalyzer queryAnalyzer = new SPARQLAnalyzer(request.getSPARQL());
		
		// create the LUTT
		lutt = new LUTT(this.getUUID(), request.getSPARQL());
		
		// create an empty model for the CTS
		ctss = new ContextTripleStore(this.getUUID());
		
		// create a CONSTRUCT from the query
		logger.debug("Building the construct");
		String construct = queryAnalyzer.getConstructFromQuery();
		
		// perform the construct query
		QueryRequest crequest = new QueryRequest(construct);
		QueryResponse qret = (QueryResponse) queryProcessor.process(crequest, 0);
		
		// Insert data retrieved with the CONSTRUCT into the local store
		logger.debug("Filling the local store");
		BindingsResults consBindings = qret.getBindingsResults();
		ctss.insertFromBindings(consBindings);
		
		// Query to the local store
		logger.debug("Querying the CTS");
		lastBindings = ctss.query(request.getSPARQL());
		firstResults = new BindingsResults(lastBindings);		

		// return
		return true;
	}

	@Override
	public Notification processInternal(UpdateResponse update) {
		logger.debug("* PROCESSING *" + request);

		// Query the SPARQL processing service
		ret = queryProcessor.process(request,0);

		if (ret.getClass().equals(ErrorResponse.class)) {
			logger.error(ret);
			return null;
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

		logger.debug("Current bindings: " + currentBindings);
		logger.debug("Last bindings: " + lastBindings);

		// Find removed bindings
		long start = System.nanoTime();
		for (Bindings solution : lastBindings.getBindings()) {
			if (!results.contains(solution) && !solution.isEmpty())
				removed.add(solution);
			else
				results.remove(solution);
		}
		long stop = System.nanoTime();
		logger.debug("Removed bindings: " + removed + " found in " + (stop - start) + " ns");

		// Find added bindings
		start = System.nanoTime();
		for (Bindings solution : results.getBindings()) {
			if (!lastBindings.contains(solution) && !solution.isEmpty())
				added.add(solution);
		}
		stop = System.nanoTime();
		logger.debug("Added bindings: " + added + " found in " + (stop - start) + " ns");

		// Update the last bindings with the current ones
		lastBindings = currentBindings;

		// Send notification (or end processing indication)
		if (!added.isEmpty() || !removed.isEmpty())
			return new Notification(getUUID(), new ARBindingsResults(added, removed), sequence++);

		return null;
	}	
	
	
	public boolean checkLutt(BindingsResults ar, BindingsResults dr) {
		
		// local variables		
		BindingsResults luttMatched_ar = lutt.checkLutt(ar);
		BindingsResults luttMatched_dr = lutt.checkLutt(dr);
		
		// return
		if ((luttMatched_ar.size() > 0) && (luttMatched_dr.size() > 0)) {
			
			logger.debug("Match found, updating local CTS");
			ctss.insertFromBindings(luttMatched_ar);			
			ctss.removeFromBindings(luttMatched_dr);								
			return true;
			
		} else {
			logger.debug("Match not found");
			return false;
		}
	}
	
	
}
