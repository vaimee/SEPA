package it.unibo.arces.wot.sepa.engine.processing;

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

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.update.UpdateFactory;
import org.apache.logging.log4j.LogManager;

public class SPUNamed extends SPU {
	private final Logger logger;

	private BindingsResults lastBindings = null;
	private Integer sequence = 0;
	private Response ret;
	private LUTT lutt = null;
	private String subns = "http://wot.arces.unibo.it/sepa#";
	private QueryRequest localizedSub;
	private UpdateProcessor updateProcessor;
	private String graphUri;

	public SPUNamed(SubscribeRequest subscribe, EventHandler handler, SPARQL11Properties endpointProperties)
			throws IllegalArgumentException, URISyntaxException {
		super(subscribe, endpointProperties, handler);
		updateProcessor = new UpdateProcessor(endpointProperties);
		logger = LogManager.getLogger("SPUNamed_" + getUUID());
		logger.debug("SPU: " + this.getUUID() + " request: " + subscribe);
		
		// read endpoint properties
		// logger.debug("Creating LUTT for subscription " + endpointProperties.getHttpPort());
	}

	@Override
	public boolean init() throws ParseException {
		logger.debug("Process SPARQL query " + request);

		// determine the graph name to be used
		graphUri = subns + "subid_" + getUUID();
		
		// manipulate query to obtain a version for the
		// named graph that will be generated later on
		// TODO - replace this one with a regexp
		localizedSub = new QueryRequest(request.getSPARQL().replaceAll("WHERE", "FROM <" + graphUri + "> WHERE"));

		// manipulate query to extract triple patterns
		String query = request.getSPARQL();
		SPARQLAnalyzer queryAnalyzer = new SPARQLAnalyzer(request.getSPARQL());

		// create the LUTT
		logger.debug("Creating LUTT for subscription " + graphUri);
		lutt = new LUTT(this.getUUID(), request.getSPARQL());		

		// create and perform a CONSTRUCT from the query in order to 
		// get the triples to put in the local triple store
		String construct = queryAnalyzer.getConstructFromQuery();
		QueryRequest crequest = new QueryRequest(construct);		
		QueryResponse qret = (QueryResponse) queryProcessor.process(crequest, 0);		
		
		// Here we have to transform the bindings returned
		// by the construct in an INSERT DATA with bindings
		// put into a graph. So:
		String sparqlUpdate = "INSERT DATA { GRAPH <" + graphUri + "> {";
		String whereSection = "";

		//  iterate over the results and put them into the sparql update
		BindingsResults bindings = qret.getBindingsResults();		
		List<Bindings> bindingsList = bindings.getBindings();
		Iterator<Bindings> bindingsIter = bindingsList.iterator();			
		
		while (bindingsIter.hasNext()) {

			// get a row and read subject, predicate and object
			Bindings el = bindingsIter.next();
			logger.debug(el.toString());
			String s = el.getBindingValue("subject");			
			String p = el.getBindingValue("predicate");
			String o = el.getBindingValue("object");

			// analyze the object to detect if it is a datatype or an object property
			// then add the property to the model (i.e. to insert a statement)
			if (el.isLiteral("object")) {
				whereSection += "<" + s + "> <" + p + "> \"" + o + "\" . ";
			} else {
				whereSection += "<" + s + "> <" + p + "> <" + o + "> . ";
			}
		}

		// finalize the SPARQL update
		sparqlUpdate += whereSection + " }}";	

		// put data into the store		
		logger.debug("Pushing initial data in the proper named graph");
		UpdateRequest u = new UpdateRequest(sparqlUpdate);		
		updateProcessor.processNoCheck(u, 0);
		
		// Query the store
		logger.debug("Querying the KB");
		ret = queryProcessor.process(localizedSub, 0);
		lastBindings = ((QueryResponse) ret).getBindingsResults();
		firstResults = new BindingsResults(lastBindings);
		
		// return
		return true;
	}

	@Override
	public Notification processInternal(UpdateResponse update) {
		logger.debug("* PROCESSING *" + request);

		// Adding in the local store the results of update		
		String localDelete = "DELETE DATA { GRAPH <" + graphUri + "> { "; 
		List<Bindings> bindings = update.removed.getBindings();
		Iterator<Bindings> bindIt = bindings.iterator();
		while (bindIt.hasNext()) {
			Bindings el = bindIt.next();						
			String s = el.getBindingValue("subject");
			String p = el.getBindingValue("predicate");
			String o = el.getBindingValue("object");
			String row = "";
			if (el.isLiteral("object")){
				row = "<" + s + "> <" + p + "> '" + o + "' . ";
			} else {
				row = "<" + s + "> <" + p + "> <" + o + "> . ";
			}
			localDelete += row;
		}
		localDelete += "}}";
		UpdateRequest u = new UpdateRequest(localDelete);
		updateProcessor.processNoCheck(u, 0);				

		// Adding in the local store the results of update		
		String localInsert = "INSERT DATA { GRAPH <" + graphUri + "> { "; 
		bindings = update.added.getBindings();
		bindIt = bindings.iterator();
		while (bindIt.hasNext()) {
			Bindings el = bindIt.next();						
			String s = el.getBindingValue("subject");
			String p = el.getBindingValue("predicate");
			String o = el.getBindingValue("object");
			String row = "";
			if (el.isLiteral("object")){
				row = "<" + s + "> <" + p + "> '" + o + "' . ";
			} else {
				row = "<" + s + "> <" + p + "> <" + o + "> . ";
			}
			localInsert += row;
		}
		localInsert += "}}";
		u = new UpdateRequest(localInsert);
		updateProcessor.processNoCheck(u, 0);		
		
		// Query the SPARQL processing service with the localized sub		
		ret = queryProcessor.process(localizedSub, 0);

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

		// returning
		return null;
	}

	public boolean checkLutt(BindingsResults ar, BindingsResults dr) {

		// local variables
		BindingsResults luttMatched_ar = null;
		BindingsResults luttMatched_dr = null;
		
		if (ar != null) {
			luttMatched_ar = lutt.checkLutt(ar);
		}
		if (dr != null) {
			luttMatched_dr = lutt.checkLutt(dr);
		}

		// return
		if (((luttMatched_ar != null) && (luttMatched_ar.size() > 0)) || ((luttMatched_dr != null) && (luttMatched_dr.size() > 0))) {
			logger.debug("Match found");
			return true;

		} else {
			logger.debug("Match not found");
			return false;
		}
	}

	@Override
	public void terminate() {				
		String delNG = "DELETE { GRAPH <" + graphUri + "> { ?s ?p ?o }} WHERE { GRAPH <" + graphUri + "> { ?s ?p ?o }}";
		UpdateRequest u = new UpdateRequest(delNG);
		updateProcessor.processNoCheck(u, 0);
		super.terminate();
	}
	
}
