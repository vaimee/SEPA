/* This class implements the scheduler of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SchedulerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.core.SchedulerRequestResponseQueue;
import it.unibo.arces.wot.sepa.engine.dependability.Timing;

/**
 * This class represents the scheduler of the SPARQL Event Processing Engine
 */

public class Scheduler extends Thread implements SchedulerMBean {
	private static final Logger logger = LogManager.getLogger("Scheduler");

	// Request tokens
	private Vector<Integer> tokens = new Vector<Integer>();

	// Responders
	private HashMap<Integer, ResponseHandler> responders = new HashMap<Integer, ResponseHandler>();

	// Synchronized queue
	private SchedulerRequestResponseQueue queue;
	
	public Scheduler(EngineProperties properties,SchedulerRequestResponseQueue queue) {
		if (properties == null) {
			logger.error("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}
		if (queue == null) {
			logger.error("Queue is null");
			throw new IllegalArgumentException("Queue is null");
		}
		this.queue = queue;
		
		// Initialize token jar
		for (int i = 0; i < properties.getSchedulingQueueSize(); i++)
			tokens.addElement(i);

		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SchedulerBeans.setQueueSize(properties.getSchedulingQueueSize());
		
		this.setName("SEPA Response Scheduler");
	}

	public synchronized void schedule(Request request, ResponseHandler handler) {
		int token = getToken();
		if (token == -1) {
			try {
				handler.sendResponse(new ErrorResponse(-1, 500, "Request refused: too many pending requests"));
			} catch (IOException e) {
				logger.error("Failed to send response on out of tokens");
			}
			return;
		}

		// Responder
		responders.put(token, handler);

		queue.addRequest(new ScheduledRequest(token, request, handler));
		
		Timing.logTiming(request, "SCHEDULED", Instant.now());
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

		logger.debug("Get token #" + token + " (Available: " + tokens.size() + ")");

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
			logger.debug("Release token #" + token + " (Available: " + tokens.size() + ")");

			SchedulerBeans.tokenLeft(tokens.size());
		}
	}

	@Override
	public void run() {
		while(true) {
			Response response;
			try {
				response = queue.waitResponse();
			} catch (InterruptedException e) {
				return;
			}
			try {
				if (responders.get(response.getToken()) != null) responders.get(response.getToken()).sendResponse(response);
			} catch (IOException e) {
				logger.error("Failed to send response: " + e.getMessage());
			}
			responders.remove(response.getToken());

			// RELEASE TOKEN
			releaseToken(response.getToken());
		}
	}

	public String getStatistics() {
		return SchedulerBeans.getStatistics();
	}

	public long getErrors() {
		return SchedulerBeans.getErrors();
	}

	public long getRequests_pending() {
		return SchedulerBeans.getQueue_Pending();
	}

	public long getRequests_max_pending() {
		return SchedulerBeans.getQueue_Max();
	}

	public long getRequests_rejected() {
		return SchedulerBeans.getQueue_OutOfToken();
	}

	@Override
	public void reset() {
		SchedulerBeans.reset();
	}

	@Override
	public long getRequests_scheduled() {
		return SchedulerBeans.getScheduledRequests();
	}

	@Override
	public int getQueueSize() {
		return SchedulerBeans.getQueueSize();
	}
}
