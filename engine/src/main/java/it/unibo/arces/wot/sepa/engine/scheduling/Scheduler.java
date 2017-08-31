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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SchedulerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;

import it.unibo.arces.wot.sepa.engine.processing.Processor;

/**
 * This class represents the scheduler of the SPARQL Event Processing Engine
 */

public class Scheduler implements Runnable, SchedulerMBean {
	private static final Logger logger = LogManager.getLogger("Scheduler");

	// Request tokens
	private Vector<Integer> tokens = new Vector<Integer>();

	// Primitive processor
	private Processor processor;

	// Request queue
	private ConcurrentLinkedQueue<ScheduledRequest> updateQueue = new ConcurrentLinkedQueue<ScheduledRequest>();
	private ConcurrentLinkedQueue<ScheduledRequest> queryQueue = new ConcurrentLinkedQueue<ScheduledRequest>();
	private ConcurrentLinkedQueue<ScheduledRequest> subscribeQueue = new ConcurrentLinkedQueue<ScheduledRequest>();
	private ConcurrentLinkedQueue<ScheduledRequest> unsubscribeQueue = new ConcurrentLinkedQueue<ScheduledRequest>();

	// Properties reference
	private EngineProperties properties;

	public Scheduler(EngineProperties properties)
			throws IllegalArgumentException, MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException, InvalidKeyException, FileNotFoundException,
			NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {
		if (properties == null) {
			logger.error("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}
		this.properties = properties;

		// Init tokens
		for (int i = 0; i < properties.getSchedulingQueueSize(); i++)
			tokens.addElement(i);

		// SPARQL 1.1 SE request processor
		processor = new Processor();

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	public synchronized int schedule(Request request, long timeout, ResponseHandler handler) {
		// Get token
		int token = getToken();
		if (token == -1) {
			SchedulerBeans.newRequest(false);
			logger.error("Request refused: too many pending requests");
		} else {
			SchedulerBeans.newRequest(true);

			if (request.getClass().equals(UpdateRequest.class)) {
				updateQueue.offer(new ScheduledRequest(token, request, timeout, handler));
			} else if (request.getClass().equals(SubscribeRequest.class)) {
				subscribeQueue.offer(new ScheduledRequest(token, request, timeout, handler));
			} else if (request.getClass().equals(QueryRequest.class)) {
				queryQueue.offer(new ScheduledRequest(token, request, timeout, handler));
			} else if (request.getClass().equals(UnsubscribeRequest.class)) {
				unsubscribeQueue.offer(new ScheduledRequest(token, request, timeout, handler));
			}

			synchronized (processor) {
				processor.notify();
			}
		}

		return token;
	}

	@Override
	public void run() {
		synchronized(Thread.currentThread()){
			Thread.currentThread().notify();
		}
		while (true) {
			synchronized (processor) {
				try {
					processor.wait();
				} catch (InterruptedException e) {

				}
			}
			
			boolean poll = false;		
			do{
				ScheduledRequest request = null;
				Response response = null;
				poll = false;
				if ((request = updateQueue.poll()) != null) {
					response = processor.process(request);
					if (request.getResponseHandler() != null)
						request.getResponseHandler().notifyResponse(response);
					releaseToken(request.getToken());
					SchedulerBeans.updateCounters(request);
					poll = true;
				}
				if ((request = subscribeQueue.poll()) != null) {
					response = processor.process(request);
					if (request.getResponseHandler() != null)
						request.getResponseHandler().notifyResponse(response);
					releaseToken(request.getToken());
					SchedulerBeans.updateCounters(request);
					poll = true;
				}
				if ((request = unsubscribeQueue.poll()) != null) {
					response = processor.process(request);
					if (request.getResponseHandler() != null)
						request.getResponseHandler().notifyResponse(response);
					releaseToken(request.getToken());
					SchedulerBeans.updateCounters(request);
					poll = true;
				}
				if ((request = queryQueue.poll()) != null) {
					response = processor.process(request);
					if (request.getResponseHandler() != null)
						request.getResponseHandler().notifyResponse(response);
					releaseToken(request.getToken());
					SchedulerBeans.updateCounters(request);
					poll = true;
				}
			} while(poll);
		}
	}

	/**
	 * Returns a new token if more tokens are available or -1 otherwise
	 * 
	 * @return an int representing the token
	 */
	private synchronized int getToken() {
		Integer token;

		if (tokens.size() == 0) {
			logger.error("No tokens available");
			return -1;
		}

		token = tokens.get(0);
		tokens.removeElementAt(0);

		logger.debug("Get token #" + token + " (Available: " + tokens.size() + ")");

		SchedulerBeans.updateQueueSize(properties.getSchedulingQueueSize(), tokens.size());

		return token;
	}

	/**
	 * Release an used token
	 * 
	 * @return true if success, false if the token to be released has not been
	 *         acquired
	 */
	private synchronized boolean releaseToken(Integer token) {
		if (token == -1)
			return false;

		boolean ret = true;

		if (tokens.contains(token)) {
			ret = false;
			logger.warn("Request to release a unused token: " + token + " (Available tokens: " + tokens.size() + ")");
		} else {
			tokens.insertElementAt(token, tokens.size());
			logger.debug("Release token #" + token + " (Available: " + tokens.size() + ")");

			SchedulerBeans.updateQueueSize(properties.getSchedulingQueueSize(), tokens.size());
		}

		return ret;
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

	public float getTimings_Update() {
		return SchedulerBeans.getTimings_Update();
	}

	public float getTimings_Query() {
		return SchedulerBeans.getTimings_Query();
	}

	public float getTimings_Subscribe() {
		return SchedulerBeans.getTimings_Subscribe();
	}

	public float getTimings_Unsubscribe() {
		return SchedulerBeans.getTimings_Unsubscribe();
	}

	@Override
	public void reset() {
		SchedulerBeans.reset();
	}

	@Override
	public long getRequests_scheduled() {
		return SchedulerBeans.getScheduledRequests();
	}
}
