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
import java.util.Observer;

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
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;

import org.apache.logging.log4j.LogManager;

public class Processor extends Observable implements Observer,ProcessorMBean {
	private static final Logger logger = LogManager.getLogger("Processor");
	
	private QueryProcessor queryProcessor;
	private UpdateProcessor updateProcessor;

	private SPUManager spuManager;
	private SPARQL11Protocol endpoint;
	
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
		spuManager.addObserver(this);
		
		SEPABeans.registerMBean("SEPA:type="+this.getClass().getSimpleName(), this);
		
		ProcessorBeans.setEndpoint(endpointProperties);
	}

	@Override
	public void update(Observable o, Object arg) {
		logger.debug("<< SPU manager " + arg.toString());
		setChanged();
		notifyObservers(arg);
	}

	public Response process(Request request) {
		logger.debug("*Process* " + request.toString());
		
		ProcessorBeans.newRequest(request);

		Response res = null;
		
		if (request.getClass().equals(UpdateRequest.class)) {
			res = updateProcessor.process((UpdateRequest) request);
			
			if (res.getClass().equals(UpdateResponse.class)){
				logger.debug("*** Subscriptions processing ***");
				spuManager.processUpdate((UpdateResponse) res);
			}
		} 
		else if (request.getClass().equals(QueryRequest.class)) {
			res = queryProcessor.process((QueryRequest) request);
		}
		else if (request.getClass().equals(SubscribeRequest.class)) {
			res = spuManager.processSubscribe((SubscribeRequest) request);
		}  
		else if (request.getClass().equals(UnsubscribeRequest.class)) {
			res = spuManager.processUnsubscribe((UnsubscribeRequest) request);
		} 
		else {
			logger.error("Unsupported request: " + request.toString());
			res = new ErrorResponse(request.getToken(), 500, "Unsupported request: " + request.toString());
		}
		
		return res;
	}

	@Override
	public String getEndpointProperties() {
		return ProcessorBeans.getEndpointProperties();
	}

	@Override
	public String getRequests() {
		return ProcessorBeans.getRequests();
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
	public String getQueryTimings() {
		return ProcessorBeans.getQueryTimings();
	}

	@Override
	public String getUpdateTimings() {
		return ProcessorBeans.getUpdateTimings();
	}
}
