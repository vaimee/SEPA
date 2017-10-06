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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SchedulerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

/**
 * This class represents the scheduler of the SPARQL Event Processing Engine
 */

public class Scheduler implements SchedulerMBean, Observer {
	private static final Logger logger = LogManager.getLogger("Scheduler");

	// Request tokens
	private Vector<Integer> tokens = new Vector<Integer>();

	// Processor
	private Processor processor;

	// Responders
	private HashMap<Integer, ResponseHandler> responders = new HashMap<Integer, ResponseHandler>();

	public Scheduler(EngineProperties properties)
			throws IllegalArgumentException, MalformedObjectNameException, InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException, InvalidKeyException, FileNotFoundException,
			NoSuchElementException, NullPointerException, ClassCastException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, URISyntaxException {
		if (properties == null) {
			logger.error("Properties are null");
			throw new IllegalArgumentException("Properties are null");
		}

		// Initialize token jar
		for (int i = 0; i < properties.getSchedulingQueueSize(); i++)
			tokens.addElement(i);

		// SPARQL 1.1 SE request processor
		SPARQL11Properties endpointProperties = new SPARQL11Properties("endpoint.jpar");
		processor = new Processor(endpointProperties,properties);
		processor.addObserver(this);

		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SchedulerBeans.setQueueSize(properties.getSchedulingQueueSize());
	}
	
	public synchronized int schedule(Request request, ResponseHandler handler) {
		if (request == null) {
			logger.error("Request is null");
			return -1;
		}

		// GET TOKEN
		int token = getToken();

		if (token == -1) {
			SchedulerBeans.newRequest(request, false);
			logger.error("Request refused: too many pending requests");
		} else {
			SchedulerBeans.newRequest(request, true);

			// Set request TOKEN
			request.setToken(token);
			responders.put(request.getToken(), handler);

			logger.debug("Schedule request: " + request);

			if (request.isUpdateRequest()) processor.processUpdate((UpdateRequest) request);
			else if (request.isSubscribeRequest()) processor.processSubscribe((SubscribeRequest) request, (EventHandler)handler);
			else if (request.isQueryRequest()) processor.processQuery((QueryRequest) request);
			else if (request.isUnsubscribeRequest()) processor.processUnsubscribe((UnsubscribeRequest) request);
		}

		return token;
	}

	@Override
	public void update(Observable o, Object arg) {	
		Response ret = (Response)arg;
		
		try {
			responders.get(ret.getToken()).sendResponse(ret);
		} catch (IOException e) {
			logger.warn("Failed to send response: " + ret);
		}
		responders.remove(ret.getToken());
		
		// RELEASE TOKEN
		releaseToken(ret.getToken());
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
