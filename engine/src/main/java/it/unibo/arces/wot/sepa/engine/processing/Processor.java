/*  This class implements the processing of the requests coming form the scheduler
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Observable;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

import org.apache.logging.log4j.LogManager;

public class Processor extends Observable implements ProcessorMBean {
	private static final Logger logger = LogManager.getLogger("Processor");

	private QueryProcessor queryProcessor;
	private UpdateProcessor updateProcessor;

	private SPUManager spuManager;
	private SPARQL11Protocol endpoint;

	private Response response;
	private Object waitResponse = new Object();

	public Processor() throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, InvalidKeyException, FileNotFoundException, NoSuchElementException,
			NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException, IllegalArgumentException, URISyntaxException {
		// Create SPARQL 1.1 interface
		SPARQL11Properties endpointProperties = new SPARQL11Properties("endpoint.jpar");
		endpoint = new SPARQL11Protocol(endpointProperties);

		// Create processor to manage (optimize) QUERY and UPDATE request
		queryProcessor = new QueryProcessor(endpoint);
		updateProcessor = new UpdateProcessor(endpoint);

		// Subscriptions manager
		spuManager = new SPUManager(endpoint);

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		ProcessorBeans.setEndpoint(endpointProperties);
	}

	public void process(ScheduledRequest request) {

		logger.debug("*Process* " + request.getRequest().toString());

		ProcessorBeans.newRequest(request.getRequest());

		response = null;
		new Thread() {
			public void run() {
				if (request.getRequest().getClass().equals(UpdateRequest.class)) {
					response = updateProcessor.process((UpdateRequest) request.getRequest());
				} else if (request.getRequest().getClass().equals(QueryRequest.class)) {
					response = queryProcessor.process((QueryRequest) request.getRequest());
				} else if (request.getRequest().getClass().equals(SubscribeRequest.class)) {
					response = spuManager.processSubscribe(request);
				} else if (request.getRequest().getClass().equals(UnsubscribeRequest.class)) {
					response = spuManager.processUnsubscribe(request);
				} else {
					logger.error("Unsupported request: " + request.getRequest().toString());
					response = new ErrorResponse(request.getToken(), 500,
							"Unsupported request: " + request.getRequest().toString());
				}
				
				synchronized(waitResponse) {
					logger.debug("Notify response: "+response.toString());
					waitResponse.notify();
				}
			}
		}.start();

		synchronized (waitResponse) {
			try {
				waitResponse.wait(request.getTimeout());
			} catch (InterruptedException e) {
			}
		}

		if (response == null)
			response = new ErrorResponse(request.getToken(), 404, request.getRequest().toString());
	
		if (response.getClass().equals(UpdateResponse.class)) {
			spuManager.processUpdate((UpdateResponse)response);
		}
		
		// Notify response
		if (request.getResponseHandler() != null) request.getResponseHandler().notifyResponse(response);
		setChanged();
		notifyObservers(response);
	}

	@Override
	public void resetQueryTimings() {
		ProcessorBeans.resetQueryTimings();
	}

	@Override
	public void resetUpdateTimings() {
		ProcessorBeans.resetUpdateTimings();
	}

	@Override
	public String getStatistics() {
		return ProcessorBeans.getStatistics();
	}

	@Override
	public float getTimings_UpdateTime_ms() {
		return ProcessorBeans.getUpdateTime_ms();
	}

	@Override
	public float getTimings_QueryTime_ms() {
		return ProcessorBeans.getQueryTime_ms();
	}
	
	@Override
	public String getEndpoint_Host() {
		return ProcessorBeans.getEndpointHost();
	}

	@Override
	public String getEndpoint_Port() {
		return String.format("%d", ProcessorBeans.getEndpointPort());
	}

	@Override
	public String getEndpoint_QueryPath() {
		return ProcessorBeans.getEndpointQueryPath();
	}

	@Override
	public String getEndpoint_UpdatePath() {
		return ProcessorBeans.getEndpointUpdatePath();
	}

	@Override
	public String getEndpoint_UpdateMethod() {
		return ProcessorBeans.getEndpointUpdateMethod();
	}

	@Override
	public String getEndpoint_QueryMethod() {
		return ProcessorBeans.getEndpointQueryMethod();
	}
}
