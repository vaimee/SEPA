/* This class implements the manager of the Semantic Processing Units (SPUs) of the Semantic Event Processing Architecture (SEPA) Engine
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

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.Logger;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class SPUManager extends Observable implements Observer, SPUManagerMBean {
	private static final Logger logger = LogManager.getLogger("SPUManager");

	private SPARQL11Properties endpointProperties;
	private EngineProperties engineProperties;

	// SPUs and SPUIDs hash map
	private HashMap<String, SPU> spus = null;

	// SPU synchronization
	private HashSet<String> processingSpus = new HashSet<String>();

	// Request queue
	private ConcurrentLinkedQueue<SPU> subscribeQueue = new ConcurrentLinkedQueue<SPU>();
	private ConcurrentLinkedQueue<SPU> unsubscribeQueue = new ConcurrentLinkedQueue<SPU>();
	private ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();

	// Response queue
	private ConcurrentLinkedQueue<Response> responseQueue = new ConcurrentLinkedQueue<Response>();

	public SPUManager(SPARQL11Properties endpointProperties, EngineProperties engineProperties) {
		this.endpointProperties = endpointProperties;
		this.engineProperties = engineProperties;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		spus = new HashMap<String, SPU>();

		SPUManagerBeans.setKeepalive(engineProperties.getKeepAlivePeriod());

		SPUActivationThread thread1 = new SPUActivationThread();
		thread1.setName("SEPA SPU Activator");
		thread1.start();

		SPUDeactivationThread thread2 = new SPUDeactivationThread();
		thread2.setName("SEPA SPU Deactivator");
		thread2.start();

		KeepaliveThread keepalive = new KeepaliveThread();
		keepalive.setName("SEPA SPU Keepalive");
		keepalive.start();

		UpdateProcessingThread updateProcessingThread = new UpdateProcessingThread();
		updateProcessingThread.setName("SEPA SPUManager");
		updateProcessingThread.start();
		
		SPUResponseThread spuResponseThread = new SPUResponseThread();
		spuResponseThread.setName("SEPA SPUManager responder");
		spuResponseThread.start();
	}

	class SPUResponseThread extends Thread {
		@Override
		public void run() {
			while (true) {
				Response ret;
				while((ret = responseQueue.poll())!=null) {
					setChanged();
					notifyObservers(ret);	
				}
				synchronized(responseQueue) {
					try {
						responseQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	}

	class KeepaliveThread extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(SPUManagerBeans.getKeepalive());
				} catch (InterruptedException e) {
					return;
				}

				synchronized (spus) {
					for (SPU spu : spus.values()) {
						try {
							spu.ping();
						} catch (Exception e) {
							// UNSUBSCRIBE SPU
							logger.error("Ping failed");
							
							synchronized (unsubscribeQueue) {
								unsubscribeQueue.offer(spu);
								unsubscribeQueue.notify();
							}
						}
					}
				}
			}
		}
	}

	public void subscribe(SubscribeRequest req, EventHandler handler) throws ParseException {
		logger.debug(req.toString());

		// TODO: choose different kinds of SPU based on subscribe request
		SPU spu = null;
		try {
			//spu = new SPUSmart(req, handler, endpointProperties);
			if (engineProperties.getCTSPolicy().equals("naive")) {
				spu = new SPUNaive(req, handler, endpointProperties);
				spu.addObserver(this);
			}
			else {
				spu = new SPUNamed(req, handler, endpointProperties);
				spu.addObserver(this);
			}
		} catch (IllegalArgumentException | URISyntaxException e) {
			logger.debug("SPU creation failed: " + e.getMessage());
			
			
			synchronized(responseQueue) {
				responseQueue.offer(new ErrorResponse(req.getToken(), 500, "SPU creation failed: " + req.toString()));
				responseQueue.notify();
			}

			return;
		}

		logger.debug("SPU init");
		if (!spu.init()) {
			logger.debug("SPU initialization failed");
						
			synchronized(responseQueue) {
				responseQueue.offer(new ErrorResponse(req.getToken(), 500, "SPU initialization failed: " + req.toString()));
				responseQueue.notify();
			}
			
			return;
		}
		
		synchronized(responseQueue) {
			responseQueue.offer(new SPUManagerNotify(
					new SubscribeResponse(req.getToken(), spu.getUUID(), req.getAlias(), spu.getFirstResults())));
			responseQueue.notify();
		}

		logger.debug("Add SPU to activation queue");
	
		synchronized (subscribeQueue) {
			subscribeQueue.offer(spu);
			subscribeQueue.notify();
		}
	}

	class SPUActivationThread extends Thread {
		@Override
		public void run() {
			while (true) {
				SPU spu;
				while ((spu = subscribeQueue.poll()) != null) {
					// Start the SPU thread
					Thread th = new Thread(spu);
					th.setName("SPU_" + spu.getUUID());
					th.start();

					synchronized (spus) {
						spus.put(spu.getUUID(), spu);
					}

					SPUManagerBeans.setActiveSPUs(spus.size());
					logger.debug(spu.getUUID() + " ACTIVATED (total: " + spus.size() + ")");
				}
				synchronized (subscribeQueue) {
					try {
						subscribeQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	}

	public void unsubscribe(UnsubscribeRequest req) {
		logger.debug(req);
		logger.debug("RICHIESTA DI UN-SOTTOSCRIZIONE");

		String spuid = req.getSubscribeUUID();

		if (!spus.containsKey(spuid)) {		
			synchronized(responseQueue) {
				responseQueue.offer(new SPUManagerNotify(new ErrorResponse(req.getToken(), 404, "SPUID not found: " + spuid)));
				responseQueue.notify();
			}

			return;
		}
		
		synchronized(responseQueue) {
			responseQueue.offer(new SPUManagerNotify(new UnsubscribeResponse(req.getToken(), spuid)));
			responseQueue.notify();
		}
		
		synchronized (unsubscribeQueue) {
			unsubscribeQueue.offer(spus.get(spuid));
			unsubscribeQueue.notify();
		}
	}

	class SPUDeactivationThread extends Thread {
		@Override
		public void run() {
			while (true) {
				SPU spu;
				while ((spu = unsubscribeQueue.poll()) != null) {
					logger.debug("Terminating: " + spu.getUUID());
					
					synchronized (spus) {
						//Terminate SPU and remove from active SPUs
						spu.terminate();
						spus.remove(spu.getUUID());
					}

					SPUManagerBeans.setActiveSPUs(spus.size());
					logger.debug("Active SPUs: " + spus.size());
				}
				synchronized (unsubscribeQueue) {
					try {
						unsubscribeQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	}

	public void process(UpdateResponse update) {
		logger.debug("* PROCESS * " + update);
		
		synchronized (updateQueue) {
			updateQueue.offer(update);
			updateQueue.notify();
		}
		
		
		
	}

	class UpdateProcessingThread extends Thread {
		@Override
		public void run() {
			while (true) {
				UpdateResponse update;
				while ((update = updateQueue.poll()) != null) {						
					
					logger.debug("*** PROCESSING SUBSCRIPTIONS *** <<< " + update);
					Instant start = Instant.now();

					// Wake-up all SPUs
					synchronized (spus) {
						logger.debug("Activate SPUs (Total: " + spus.size() + ")");
						processingSpus.clear();
						for (SPU spu : spus.values()) {
//							processingSpus.add(spu.getUUID());
							if (spu instanceof SPUSmart) {
								processingSpus.add(spu.getUUID());
								SPUSmart spusm = (SPUSmart) spu;
//								if (spusm.checkLutt(update.added, update.removed)) {									
//									spu.process(update);
//								};
							}								
							else if (spu instanceof SPUNamed) {								
								SPUNamed spusm = (SPUNamed) spu;
								if (spusm.checkLutt(update.added, update.removed)) {	
									processingSpus.add(spusm.getUUID());
									logger.debug("Starting spu.process");
									spusm.process(update);
								};
							}
							else {
								processingSpus.add(spu.getUUID());
								SPUNaive spusm = (SPUNaive) spu;
								spusm.process(update);
							}
						}
					}

					// Wait all SPUs completing processing
					synchronized (processingSpus) {
						logger.debug("Wait SPUs to complete processing...");
						while (!processingSpus.isEmpty()) {
							try {
								processingSpus.wait();
							} catch (InterruptedException e) {
								return;
							}
						}
					}

					Instant stop = Instant.now();
					SPUManagerBeans.timings(start, stop);

					int ms = stop.get(ChronoField.MILLI_OF_SECOND) - start.get(ChronoField.MILLI_OF_SECOND);
					if (ms < 1000)
						logger.debug("SPUs processing time: " + ms + " ms");
					else
						logger.warn("SPUs processing time: " + ms + " ms");
					
					synchronized(responseQueue) {
						responseQueue.offer(new SPUManagerNotify());
						responseQueue.notify();
					}

				}
				synchronized (updateQueue) {
					try {
						updateQueue.wait();
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		SPU spu = (SPU) o;

		synchronized (processingSpus) {
			processingSpus.remove(spu.getUUID());
			logger.debug("SPUs left: " + processingSpus.size());
			processingSpus.notify();
		}

		if (!spu.isRunning()) {
			logger.debug("*** SPU terminated: " + spu.getUUID() + " ***");
			synchronized (spus) {
				spus.remove(spu.getUUID());
			}
		}
	}

	@Override
	public long getRequests() {
		return SPUManagerBeans.getRequests();
	}

	@Override
	public long getSPUs_current() {
		return SPUManagerBeans.getSPUs_current();
	}

	@Override
	public long getSPUs_max() {
		return SPUManagerBeans.getSPUs_max();
	}

	@Override
	public float getSPUs_time() {
		return SPUManagerBeans.getSPUs_time();
	}

	@Override
	public void reset() {
		SPUManagerBeans.reset();
	}

	@Override
	public void setKeepalive(int t) {
		SPUManagerBeans.setKeepalive(t);
	}

	@Override
	public int getKeepalive() {
		return SPUManagerBeans.getKeepalive();
	}

	@Override
	public float getSPUs_time_min() {
		return SPUManagerBeans.getSPUs_time_min();
	}

	@Override
	public float getSPUs_time_max() {
		return SPUManagerBeans.getSPUs_time_max();
	}

	@Override
	public float getSPUs_time_average() {
		return SPUManagerBeans.getSPUs_time_averaae();
	}
}
