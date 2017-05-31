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

import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

/**
 * This class represents a Semantic Processing Unit (SPU)
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public abstract class SPU extends Observable implements Runnable {
	private static final Logger logger = LogManager.getLogger("SPU");	
	
	//The URI of the subscription (i.e., sepa://subscription/UUID)
	private String uuid = null;
	private String prefix = "sepa://subscription/";
	
	//Update queue
	protected ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();
	
	//Subscription
	protected QueryProcessor queryProcessor = null;
	protected SubscribeRequest subscribe = null;	
	
	//Thread loop
	private boolean running = true;

	class SubscriptionProcessingInputData {
		public UpdateResponse update = null;
		public QueryProcessor queryProcessor = null;
		public SubscribeRequest subscribe = null;	
	}
	
	public SPU(SubscribeRequest subscribe,SPARQL11Protocol endpoint) {
		uuid = prefix + UUID.randomUUID().toString();
		this.subscribe = subscribe;
		this.queryProcessor = new QueryProcessor(endpoint);
		
		//spuData.offer(subscription);
	}
	
	public synchronized void stopRunning() {
		running = false;
		notifyAll();
	}
	
	public String getUUID() {
		return uuid;
	}
	
	//To be implemented by specific implementations
	public abstract void init();
	public abstract Notification process(UpdateResponse update);
	
	public synchronized void subscriptionCheck(UpdateResponse res) {
		//subscription.update = res;
		updateQueue.offer(res);
		notifyAll();
	}
	
	private synchronized UpdateResponse waitUpdate() {
		while(updateQueue.isEmpty()){
			try {
				logger.debug(getUUID() + " Waiting new update response...");
				wait();
			} catch (InterruptedException e) {}
			
			if (!running) return null;
		}
		
		return updateQueue.poll();	
	}
	
	@Override
	public void run() {
		//Initialize the subscription (e.g., retrieve the first results) 
		init();
		
		//Main loop
		logger.debug(getUUID()+" Entering main loop...");
		while(running){			
			//Wait new update
			UpdateResponse update = waitUpdate();
			
			if (update == null && !running) return;
			
			//Processing
			Notification result = process(update);
			
			//Results notification
			setChanged();
			notifyObservers(result);
		}	
	}
}
