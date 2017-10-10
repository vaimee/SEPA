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
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;

import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.ProcessorBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

import org.apache.logging.log4j.LogManager;

public class Processor extends Observable implements ProcessorMBean, Observer {
	private static final Logger logger = LogManager.getLogger("Processor");

	// Processors
	private final UpdateProcessor updateProcessor;
	private final QueryProcessor queryProcessor;
	private SPUManager spuManager;

	private boolean updateProcessing = true;

	// Update queue
	private ConcurrentLinkedQueue<UpdateRequest> updateRequestQueue = new ConcurrentLinkedQueue<UpdateRequest>();

	public Processor(SPARQL11Properties endpointProperties, EngineProperties properties)
			throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException,
			NotCompliantMBeanException, InvalidKeyException, FileNotFoundException, NoSuchElementException,
			NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException, IOException, IllegalArgumentException, URISyntaxException {

		// Update processor
		updateProcessor = new UpdateProcessor(endpointProperties);

		// Query processor
		queryProcessor = new QueryProcessor(endpointProperties);

		// SPU manager
		spuManager = new SPUManager(endpointProperties, properties);
		spuManager.addObserver(this);
		
		// JMX
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		ProcessorBeans.setEndpoint(endpointProperties);
		ProcessorBeans.setQueryTimeout(properties.getQueryTimeout());
		ProcessorBeans.setUpdateTimeout(properties.getUpdateTimeout());

		Thread th = new Thread() {
			@Override
			public void run() {
				while (true) {
					UpdateRequest request;
					while ((request = updateRequestQueue.poll()) != null) {
						logger.debug("New request: " + request);

						// Process update request
						Response ret = updateProcessor.process(request, ProcessorBeans.getUpdateTimeout());

						// Notify update result
						setChanged();
						notifyObservers(ret);

						if (ret.isUpdateResponse()) {
							updateProcessing = true;

							spuManager.process((UpdateResponse) ret);

							while (updateProcessing) {
								// Wait for SPUs processing end
								synchronized (updateProcessor) {
									try {
										updateProcessor.wait();
									} catch (InterruptedException e) {
										return;
									}
								}
							}
						}
					}

					synchronized (updateRequestQueue) {
						try {
							updateRequestQueue.wait();
						} catch (InterruptedException e) {
							logger.error(e.getMessage());
							return;
						}
					}
				}

			}
		};
		th.setName("SEPA Processor");
		th.start();
	}
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg.getClass().equals(SPUEndOfProcessing.class)) {
			// UPDATE PROCESSING ENDED or TIMEOUT
			SPUEndOfProcessing ret = (SPUEndOfProcessing) arg;
			if (ret.isTimeout()) logger.error("SPU processing timeout");
			
			synchronized (updateProcessor) {
				updateProcessing = false;
				updateProcessor.notify();
			}
		} else if (arg.getClass().equals(ScheduledRequest.class)) {
			// NEW PROCESSING REQUEST
			Request request = ((ScheduledRequest) arg).getRequest();
			if (request.isUpdateRequest()) {
				logger.info("Update request #" + request.getToken());
				logger.debug(request);

				synchronized (updateRequestQueue) {
					updateRequestQueue.offer((UpdateRequest) request);
					updateRequestQueue.notify();
				}
			} else if (request.isQueryRequest()) {
				logger.info("Query request #" + request.getToken());
				logger.debug(request);

				Thread queryProcessing = new Thread() {
					public void run() {
						Response ret = queryProcessor.process((QueryRequest) request, ProcessorBeans.getQueryTimeout());

						setChanged();
						notifyObservers(ret);
					}
				};
				queryProcessing.setName("SEPA Query Processing Thread-" + request.getToken());
				queryProcessing.start();
			} else if (request.isSubscribeRequest()) {
				logger.info("Subscribe request #" + request.getToken());
				logger.debug(request);

				Response ret = spuManager.subscribe((SubscribeRequest) request,
						(EventHandler) ((ScheduledRequest) arg).getHandler());

				setChanged();
				notifyObservers(ret);
			} else if (request.isUnsubscribeRequest()) {
				logger.info("Unsubscribe request #" + request.getToken());
				logger.debug(request);

				Response ret = spuManager.unsubscribe((UnsubscribeRequest) request);

				setChanged();
				notifyObservers(ret);
			}
		}
	}

	@Override
	public void reset() {
		ProcessorBeans.reset();
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
	public long getProcessedRequests() {
		return ProcessorBeans.getProcessedRequests();
	}

	@Override
	public long getProcessedQueryRequests() {
		return ProcessorBeans.getProcessedQueryRequests();
	}

	@Override
	public long getProcessedUpdateRequests() {
		return ProcessorBeans.getProcessedUpdateRequests();
	}

	@Override
	public float getTimings_UpdateTime_Min_ms() {
		return ProcessorBeans.getTimings_UpdateTime_Min_ms();
	}

	@Override
	public float getTimings_UpdateTime_Average_ms() {
		return ProcessorBeans.getTimings_UpdateTime_Average_ms();
	}

	@Override
	public float getTimings_UpdateTime_Max_ms() {
		return ProcessorBeans.getTimings_UpdateTime_Max_ms();
	}

	@Override
	public float getTimings_QueryTime_Min_ms() {
		return ProcessorBeans.getTimings_QueryTime_Min_ms();
	}

	@Override
	public float getTimings_QueryTime_Average_ms() {
		return ProcessorBeans.getTimings_QueryTime_Average_ms();
	}

	@Override
	public float getTimings_QueryTime_Max_ms() {
		return ProcessorBeans.getTimings_QueryTime_Max_ms();
	}

	@Override
	public int getUpdateTimeout() {
		return ProcessorBeans.getUpdateTimeout();
	}

	@Override
	public int getQueryTimeout() {
		return ProcessorBeans.getQueryTimeout();
	}

	@Override
	public void setUpdateTimeout(int t) {
		ProcessorBeans.setUpdateTimeout(t);
	}

	@Override
	public void setQueryTimeout(int t) {
		ProcessorBeans.setQueryTimeout(t);
	}
}
