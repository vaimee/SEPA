/* This class implements the processing of a SPARQL 1.1 UPDATE
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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.jena.query.*;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.Update;
import org.apache.jena.update.UpdateFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;

public class UpdateProcessor {
	private static final Logger logger = LogManager.getLogger("UpdateProcessor");

	private SPARQL11Properties properties;
		
	public UpdateProcessor(SPARQL11Properties properties) {
		this.properties = properties;
	}

	public synchronized Response process(UpdateRequest req, int timeout) {
		logger.debug("* PROCESSING *");
		
		// Check the endpoint
		SPARQL11Protocol endpoint = null;
		try {
			endpoint = new SPARQL11Protocol(properties);
		} catch (IllegalArgumentException | URISyntaxException e) {
			// return new ErrorResponse(req.getToken(),500,e.getMessage());
			return new ErrorResponse(req.getToken(),500,e.getMessage());			
		}
			
		// Get the constructs needed to determine added and removed data
		HashMap<String,String> constructs = null;
		org.apache.jena.update.UpdateRequest updateRequest = UpdateFactory.create(req.getSPARQL());
		List<Update> updateList = updateRequest.getOperations();
		Iterator<Update> updateListIterator = updateList.iterator();
		while (updateListIterator.hasNext()) {
			Update u2 = updateListIterator.next();
			SPARQLAnalyzer sa = new SPARQLAnalyzer(u2.toString());		
			constructs = sa.getConstruct();
		}

		// Perform the requests
		Response ret;
		BindingsResults added = null;
		BindingsResults removed = null;
		String dc = constructs.get("DeleteConstruct");		
		if (dc.length() > 0) {			
			QueryRequest cons1 = new QueryRequest(dc);					
			logger.debug(cons1.toString());
			removed = ((QueryResponse) endpoint.query(cons1, timeout)).getBindingsResults();			
			logger.debug(removed);
		}
		else {
			removed = new BindingsResults(new JsonObject());
		}
		String ac = constructs.get("InsertConstruct");		
		if (ac.length() > 0) {
			QueryRequest cons2 = new QueryRequest(ac);			
			logger.debug(cons2.toString());
			added = ((QueryResponse) endpoint.query(cons2, timeout)).getBindingsResults();			
			logger.debug(added);			
		}
		else {
			added = new BindingsResults(new JsonObject());
		}
		
		// UPDATE the endpoint
		long start = System.currentTimeMillis();		
		ret = endpoint.update(req, timeout);		
		if (ret.isUpdateResponse()) {	
			ret.added = added;
			ret.removed = removed;
			logger.debug(added);
			logger.debug(removed);
		}
		long stop = System.currentTimeMillis();	
		ProcessorBeans.updateTimings(start, stop);
		
		logger.debug(ret.isUpdateResponse());
		
		// Return
		return ret;
	}
	
	public synchronized Response processNoCheck(UpdateRequest req, int timeout) {
		logger.debug("* PROCESSING *");
		
		// Check the endpoint
		SPARQL11Protocol endpoint = null;
		try {
			endpoint = new SPARQL11Protocol(properties);
		} catch (IllegalArgumentException | URISyntaxException e) {
			return new ErrorResponse(req.getToken(),500,e.getMessage());			
		}			
		
		// UPDATE the endpoint
		long start = System.currentTimeMillis();		
		Response ret = endpoint.update(req, timeout);		
		long stop = System.currentTimeMillis();	
		ProcessorBeans.updateTimings(start, stop);
		
		logger.debug(ret.isUpdateResponse());
		
		// Return
		return ret;
	}
	
}
