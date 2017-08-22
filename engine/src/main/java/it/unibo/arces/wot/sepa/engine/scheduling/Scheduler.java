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

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Observer;
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

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SchedulerBeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

/**
 * This class represents the scheduler of the SPARQL Event Processing Engine
 */

public class Scheduler extends Observable implements Runnable, Observer, SchedulerMBean {
	private static final Logger logger = LogManager.getLogger("Scheduler");

	// Request tokens
	private Vector<Integer> tokens = new Vector<Integer>();

	// Primitive processor
	private Processor processor;

	// Request queue
	private ConcurrentLinkedQueue<ScheduledRequest> requestQueue = new ConcurrentLinkedQueue<ScheduledRequest>();

	// Subscribers
	private HashMap<String, ResponseAndNotificationListener> subscribers = new HashMap<String, ResponseAndNotificationListener>();

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
		processor.addObserver(this);

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
	}

	@Override
	public void update(Observable o, Object arg) {
		// Notification
		if (arg.getClass().equals(Notification.class)) {
			Notification notify = (Notification) arg;
			logger.debug("<< notify: " + notify.toString());

			subscribers.get(notify.getSPUID()).notify(notify);
		}
	}

	public int schedule(Request request, ResponseAndNotificationListener listener) {
		// Get token
		int token = getToken();
		if (token == -1) {
			SchedulerBeans.newRequest(true);
			
			logger.error("Too many pending requests");
			listener.notify(new ErrorResponse(request.getToken(), 500, "Too many pending requests"));
		} 
		else {
			SchedulerBeans.newRequest(false);

			synchronized (requestQueue) {
				requestQueue.offer(new ScheduledRequest(token, request, listener));
				requestQueue.notify();
			}
		}

		return token;
	}

	@Override
	public void run() {
		while (true) {
			ScheduledRequest request = null;
			while ((request = requestQueue.poll()) == null) {
				synchronized (requestQueue) {
					try {
						requestQueue.wait();
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
					}
				}
			}

			// Request processing
			Response ret = processor.process(request.getRequest());

			// JMX
			SchedulerBeans.updateCounters(request);

			// Release token
			releaseToken(ret.getToken());

			// Response notification
			if (request.getListener() != null)
				request.getListener().notify(ret);

			// Subscriptions
			if (ret.getClass().equals(SubscribeResponse.class)) {
				if (request.getListener() != null)
					subscribers.put(((SubscribeResponse) ret).getSpuid(), request.getListener());
			} else if (ret.getClass().equals(UnsubscribeResponse.class)) {
				subscribers.remove(((UnsubscribeResponse) ret).getSpuid());
			}
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

	@Override
	public String getRequests() {
		return SchedulerBeans.getRequests();
	}

	@Override
	public String getPendingRequests() {
		return SchedulerBeans.getPendingRequests();
	}

	@Override
	public String getUpdateTimings() {
		return SchedulerBeans.getUpdateTimings();
	}

	@Override
	public String getQueryTimings() {
		return SchedulerBeans.getQueryTimings();
	}

	@Override
	public String getSubscribeTimings() {
		return SchedulerBeans.getSubscribeTimings();
	}

	@Override
	public String getUnsubscribeTimings() {
		return SchedulerBeans.getUnsubscribeTimings();
	}

	@Override
	public void reset() {
		SchedulerBeans.reset();
	}
}
