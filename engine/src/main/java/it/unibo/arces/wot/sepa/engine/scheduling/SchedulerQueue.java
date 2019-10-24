/* Scheduling queues
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

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.bean.SchedulerBeans;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

class SchedulerQueue {
	private static final Logger logger = LogManager.getLogger();
	
	// Tokens
	private final static Vector<Integer> tokens = new Vector<Integer>();

	// Requests
	private final static LinkedBlockingQueue<ScheduledRequest> updates = new LinkedBlockingQueue<ScheduledRequest>();
	private final static LinkedBlockingQueue<ScheduledRequest> queries = new LinkedBlockingQueue<ScheduledRequest>();
	private final static LinkedBlockingQueue<ScheduledRequest> subscribesUnsubscribes = new LinkedBlockingQueue<ScheduledRequest>();
	
	// Broken subscriptions
	private final static LinkedBlockingQueue<String> toBeKilled = new LinkedBlockingQueue<String>();
	
	// Responses
	private final static LinkedBlockingQueue<ScheduledResponse> responses = new LinkedBlockingQueue<ScheduledResponse>();

	public SchedulerQueue(long size) {
		// Initialize token jar
		for (int i = 0; i < size; i++)
			tokens.addElement(i);
	}

	/**
	 * Returns a new token if more tokens are available or -1 otherwise
	 * 
	 * @return an int representing the token
	 */
	private synchronized int getToken() {
		if (tokens.size() == 0) {
			logger.error("No tokens available");
			return -1;
		}

		Integer token = tokens.get(0);
		tokens.removeElementAt(0);

		logger.trace("Get token #" + token + " (Available: " + tokens.size() + ")");

		SchedulerBeans.tokenLeft(tokens.size());

		return token;
	}

	/**
	 * Release an used token
	 * 
	 * @return true if success, false if the token to be released has not been
	 *         acquired
	 */
	private synchronized void releaseToken(Integer token) {
		if (token == -1)
			return;

		if (tokens.contains(token)) {
			logger.warn("Request to release a unused token: " + token + " (Available tokens: " + tokens.size() + ")");
		} else {
			tokens.insertElementAt(token, tokens.size());
			logger.trace("Release token #" + token + " (Available: " + tokens.size() + ")");

			SchedulerBeans.tokenLeft(tokens.size());
		}
	}
	
	public ScheduledRequest addRequest(InternalRequest req,ResponseHandler handler) {
		int token = getToken();
		if (token == -1)  return null;
		
		ScheduledRequest request = new ScheduledRequest(token,req,handler);
		
		if (req.isUpdateRequest()) updates.add(request);
		else if (req.isQueryRequest()) queries.add(request);
		else if (req.isSubscribeRequest()) subscribesUnsubscribes.add(request);
		else if (req.isUnsubscribeRequest())subscribesUnsubscribes.add(request);
		
		return request;
	}

	public ScheduledRequest waitUpdateRequest() throws InterruptedException {
		return updates.take();
	}
	
	public ScheduledRequest waitQueryRequest() throws InterruptedException {
		return queries.take();
	}
	
	public ScheduledRequest waitSubscribeUnsubscribeRequest() throws InterruptedException {
		return subscribesUnsubscribes.take();
	}
	
	public ScheduledResponse waitResponse() throws InterruptedException {
		return responses.take();
	}

	public void addResponse(int token,Response res) {
		releaseToken(token);
		responses.offer(new ScheduledResponse(token,res));
	}	

	public void killSpuid(String spuid) {
		toBeKilled.offer(spuid);
	}

	public String waitSpuid2Kill() throws InterruptedException {
		return toBeKilled.take();
	}
}
