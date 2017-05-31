/* This class implements the interface between the input gates and the scheduler/SPUs
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

package it.unibo.arces.wot.sepa.engine.scheduling;

import java.util.HashMap;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

/**
 * This class implements the handler of the different requests: QUERY, UPDATE, SUBSCRIBE, UNSUBSCRIBE
 * 
 * It also used to notify interested listeners of new responses
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class RequestResponseHandler {
	private static final Logger logger = LogManager.getLogger("RequestResponseHandler");

	public interface ResponseAndNotificationListener {
		public void notify(Response response);
	}
	
	//Request queues
	private ConcurrentLinkedQueue<UpdateRequest> 		updateRequestQueue = new ConcurrentLinkedQueue<UpdateRequest>();
	private ConcurrentLinkedQueue<SubscribeRequest> 	subscribeRequestQueue = new ConcurrentLinkedQueue<SubscribeRequest>();
	private ConcurrentLinkedQueue<QueryRequest> 		queryRequestQueue = new ConcurrentLinkedQueue<QueryRequest>();
	private ConcurrentLinkedQueue<UnsubscribeRequest> 	unsubscribeRequestQueue = new ConcurrentLinkedQueue<UnsubscribeRequest>();
	
	//Update response queue
	private ConcurrentLinkedQueue<UpdateResponse> 		updateResponseQueue = new ConcurrentLinkedQueue<UpdateResponse>();
	
	//Response listeners
	private HashMap<Integer,ResponseAndNotificationListener> responseListeners = new HashMap<Integer,ResponseAndNotificationListener>();
	
	//Subscribers
	private HashMap<String,ResponseAndNotificationListener> subscribers = new HashMap<String,ResponseAndNotificationListener>();
	
	public RequestResponseHandler(EngineProperties properties) throws IllegalArgumentException {
		if (properties == null) logger.error("Properties are null");
		if (properties == null) throw new IllegalArgumentException("Engine properties is null"); 
	}
		
	
	/**
	 * This method add a response (e.g, UPDATE, QUERY, SUBSCRIBE, UNSUBSCRIBE)
	 * 
	 * @see Response
	* */
	public void addResponse(Response response) {
		logger.debug("<< " + response.toString());
		
		//Get listener
		ResponseAndNotificationListener listener = responseListeners.get(response.getToken());
		
		if (response.getClass().equals(SubscribeResponse.class)) {			
			subscribers.put(((SubscribeResponse) response).getSpuid(),listener);
		}
		else if (response.getClass().equals(UpdateResponse.class)) {	
			synchronized(updateResponseQueue) {
				updateResponseQueue.offer((UpdateResponse)response);
				updateResponseQueue.notifyAll();
			}
		}
		else if (response.getClass().equals(UnsubscribeResponse.class)) {
			subscribers.remove(((UnsubscribeResponse) response).getSpuid());
		}
		
		//Notify listener
		if (listener != null) listener.notify(response);
		responseListeners.remove(response.getToken());
	}
	
	/**
	 * This method add a notification sent by a SPU
	 * 
	 * @see Notification
	* */
	public void addNotification(Notification notification) {
		logger.debug("<< " + notification.toString());

		ResponseAndNotificationListener listener = subscribers.get(notification.getSPUID());
		if (listener != null) listener.notify(notification);
	}
	
	/**
	 * This method is used by producers (e.g. HTTP Gate) to add a request (e.g, UPDATE, QUERY, SUBSCRIBE, UNSUBSCRIBE). The registered listener will receive a notification when the request will be completed
	 * 
	 * @see Request, ResponseListener
	* */
	public void addRequest(Request req,ResponseAndNotificationListener listener) {
		logger.debug(">> "+req.toString());
		
		//Register response listener
		responseListeners.put(req.getToken(), listener);
		
		//Add request to the right queue
		if (req.getClass().equals(QueryRequest.class)) {
			
			synchronized(queryRequestQueue) {
				queryRequestQueue.offer((QueryRequest)req);
				queryRequestQueue.notifyAll();
			}
		}
		else if (req.getClass().equals(UpdateRequest.class)) {
			
			synchronized(updateRequestQueue) {
				updateRequestQueue.offer((UpdateRequest)req);
				updateRequestQueue.notifyAll();
			}
		}
		else if (req.getClass().equals(SubscribeRequest.class)) {
			
			synchronized(subscribeRequestQueue) {
				subscribeRequestQueue.offer((SubscribeRequest)req);
				subscribeRequestQueue.notifyAll();
			}
		}
		else {
			
			synchronized(unsubscribeRequestQueue) {
				unsubscribeRequestQueue.offer((UnsubscribeRequest)req);
				unsubscribeRequestQueue.notifyAll();
			}
		}
	}
	
	/**
	 * This method blocks until a new UPDATE response has been added
	 * 
	 * @see UpdateResponse
	* */
	public UpdateResponse waitUpdateResponse() {
		UpdateResponse res;
			
		synchronized(updateResponseQueue) {
			while((res = updateResponseQueue.poll()) == null)
				try {
					logger.debug("Waiting for UPDATE responses...");
					updateResponseQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return res;
	}
	
	/**
	 * This method blocks until a new QUERY request has been added
	 * 
	 * @see QueryRequest
	* */
	public QueryRequest waitQueryRequest() {
		QueryRequest req;
		
		synchronized(queryRequestQueue) {
			while((req = queryRequestQueue.poll()) == null)
				try {
					logger.debug("Waiting for QUERY requests...");
					queryRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new UPDATE request has been added
	 * 
	 * @see UpdateRequest
	* */
	public UpdateRequest waitUpdateRequest() {
		UpdateRequest req;
		
		synchronized(updateRequestQueue) {
			while((req = updateRequestQueue.poll()) == null)
				try {
					logger.debug("Waiting for UPDATE requests...");
					updateRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new SUBSCCRIBE request has been added
	 * 
	 * @see SubscribeRequest
	* */
	public SubscribeRequest waitSubscribeRequest() {
		SubscribeRequest req;
		
		synchronized(subscribeRequestQueue) {
			while((req = subscribeRequestQueue.poll()) == null)
				try {
					logger.debug("Waiting for SUBSCRIBE requests...");
					subscribeRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
	
	/**
	 * This method blocks until a new UNSUBSCCRIBE request has been added
	 * 
	 * @see UnsubscribeRequest
	* */

	public UnsubscribeRequest waitUnsubscribeRequest() {
		UnsubscribeRequest req;
		
		synchronized(unsubscribeRequestQueue) {
			while((req = unsubscribeRequestQueue.poll()) == null)
				try {
					logger.debug("Waiting for UNSUBSCRIBE requests...");
					unsubscribeRequestQueue.wait();
				} catch (InterruptedException e) {}
		}
		
		return req;
	}
}
