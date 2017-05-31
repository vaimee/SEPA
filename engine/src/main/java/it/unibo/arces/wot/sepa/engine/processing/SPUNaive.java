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
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

import org.apache.logging.log4j.LogManager;

public class SPUNaive extends SPU{
	private BindingsResults lastBindings;
	private Integer sequence = 0;
	private static final Logger logger = LogManager.getLogger("SPUNaive");
	
	public SPUNaive(SubscribeRequest subscribe, SPARQL11Protocol endpoint) {
		super(subscribe, endpoint);
	}

	@Override
	public void init() {			
		//Process the subscribe SPARQL query
		Response ret = queryProcessor.process(subscribe);
		if (ret.getClass().equals(ErrorResponse.class)) {
			logger.error(ret.toString());
			setChanged();
			notifyObservers(ret);
			return;		
		}
		
		//Notify subscription ID (SPU ID)
		logger.debug(ret.toString());
		SubscribeResponse response = new SubscribeResponse(subscribe.getToken(),getUUID(),subscribe.getAlias());
 
		setChanged();
		notifyObservers(response);
		
		//Get first query results
		QueryResponse queryResults = (QueryResponse) ret;
		
		//Notify bindings
		lastBindings = queryResults.getBindingsResults();
		if (!lastBindings.isEmpty()) {
			ARBindingsResults bindings =  new ARBindingsResults(lastBindings,null);
			Notification notification = new Notification(getUUID(),bindings,sequence++);
			setChanged();
			notifyObservers(notification);	
		}	
	}

	@Override
	public Notification process(UpdateResponse update) {
				
		logger.debug("Start processing "+this.getUUID());
		
		//Query the SPARQL processing service
		Response ret = queryProcessor.process(subscribe);
		
		if (ret.getClass().equals(ErrorResponse.class)) {
			logger.error(ret.toString());
			return new Notification(getUUID(),null,0);	
		}
		
		QueryResponse currentResults = (QueryResponse) ret;
		
		//Current and previous bindings
		BindingsResults currentBindings = currentResults.getBindingsResults();
		BindingsResults newBindings = new BindingsResults(currentBindings);
		
		//Initialize the results with the current bindings
		BindingsResults added = new BindingsResults(currentBindings.getVariables(),null);
		BindingsResults removed = new BindingsResults(currentBindings.getVariables(),null);
		
		//Create empty bindings if null
		if (lastBindings == null) lastBindings = new BindingsResults(null,null);
		
		//Find removed bindings
		for(Bindings solution : lastBindings.getBindings()) {
			if(!currentBindings.contains(solution) && !solution.isEmpty()) removed.add(solution);
			else currentBindings.remove(solution);	
		}
		
		//Find added bindings
		for(Bindings solution : currentBindings.getBindings()) {
			if(!lastBindings.contains(solution) && !solution.isEmpty()) added.add(solution);	
		}
		
		//Update the last bindings with the current ones
		lastBindings = new BindingsResults(newBindings);
				
		//Send notification (or end processing indication)
		Notification notification = null;
		if (!added.isEmpty() || !removed.isEmpty()){
			ARBindingsResults bindings =  new ARBindingsResults(added,removed);
			notification = new Notification(getUUID(),bindings,sequence++);
		}
		else notification = new Notification(getUUID(),null,0);
		
		logger.debug( getUUID() + " End processing");
		
		return notification;	
	}

}
