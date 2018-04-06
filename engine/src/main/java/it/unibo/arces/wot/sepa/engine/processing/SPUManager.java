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

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;

import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;
import it.unibo.arces.wot.sepa.engine.core.EventHandler;

public class SPUManager implements SPUManagerMBean {
	private final Logger logger = LogManager.getLogger("SPUManager");

	private SPARQL11Properties endpointProperties;

	// SPUs and SPUIDs hash map
	private HashMap<String, SPU> spus = new HashMap<String, SPU>();

	// Request queue
	private ConcurrentLinkedQueue<SPU> subscribeQueue = new ConcurrentLinkedQueue<SPU>();
	private ConcurrentLinkedQueue<SPU> unsubscribeQueue = new ConcurrentLinkedQueue<SPU>();
	private ConcurrentLinkedQueue<UpdateResponse> updateQueue = new ConcurrentLinkedQueue<UpdateResponse>();

	private Semaphore endpointSemaphore;

	// SPU Synchronization
	private SPUSync spuSync = new SPUSync();

	public SPUManager(SPARQL11Properties endpointProperties, EngineProperties engineProperties,
			Semaphore endpointSemaphore, UpdateProcessingQueue updateProcessingQueue) {
		this.endpointProperties = endpointProperties;
		this.endpointSemaphore = endpointSemaphore;

		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);
		SPUManagerBeans.setKeepalive(engineProperties.getKeepAlivePeriod());
		SPUManagerBeans.setSPUProcessingTimeout(engineProperties.getSPUProcessingTimeout());

		// Unsubscribe request activator thread
		Thread thread1 = new Thread() {
			public void run() {
				while (true) {
					SPU spu;
					while ((spu = unsubscribeQueue.poll()) != null) {
						logger.debug("Terminating: " + spu.getUUID());

						synchronized (spus) {
							// Terminate SPU and remove from active SPUs
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
		};
		thread1.setName("SEPA SPU Unsubscribe");
		thread1.start();

		// Subscribe request activator thread
		Thread thread2 = new Thread() {
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
		};
		thread2.setName("SEPA SPU Subscribe");
		thread2.start();

		// Broken subscriptions detector thread
		// Thread keepalive = new Thread() {
		// public void run() {
		// while (true) {
		// try {
		// Thread.sleep(SPUManagerBeans.getKeepalive());
		// } catch (InterruptedException e) {
		// return;
		// }
		//
		// synchronized (spus) {
		// for (SPU spu : spus.values()) {
		// try {
		// spu.ping();
		// } catch (Exception e) {
		// // UNSUBSCRIBE SPU
		// logger.warn("Ping failed");
		//
		// synchronized (unsubscribeQueue) {
		// unsubscribeQueue.offer(spu);
		// unsubscribeQueue.notify();
		// }
		// }
		// }
		// }
		// }
		// }
		// };
		// keepalive.setName("SEPA SPU Keepalive");
		// keepalive.start();

		// Main update processing thread
		Thread main = new Thread() {
			public void run() {
				while (true) {
					UpdateResponse update;
					while ((update = updateQueue.poll()) != null) {
						logger.info("*** PROCESSING SUBSCRIPTIONS BEGIN *** ");
						Instant start = Instant.now();

						// Wake-up all SPUs
						synchronized (spus) {
							logger.info("Activate SPUs (Total: " + spus.size() + ")");

							spuSync.startProcessing(spus.values());

							for (SPU spu : spus.values())
								spu.process(update);
						}

						// Wait all SPUs completing processing (or timeout)
						spuSync.waitEndOfProcessing();

						Instant stop = Instant.now();
						SPUManagerBeans.timings(start, stop);

						// Notify processor of end of processing
						updateProcessingQueue.updateEOP(new SPUEndOfProcessing(!spuSync.isEmpty()));

						logger.info("*** PROCESSING SUBSCRIPTIONS END *** ");
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
		};
		main.setName("SEPA SPU Manager");
		main.start();

	}

	public Response subscribe(SubscribeRequest req, EventHandler handler) {
		logger.debug(req.toString());

		SPUManagerBeans.subscribeRequest();

		// TODO: choose different kinds of SPU based on subscribe request
		SPU spu = null;
		try {
			spu = new SPUNaive(req, handler, endpointProperties, endpointSemaphore, spuSync);
		} catch (SEPAProtocolException e) {
			logger.debug("SPU creation failed: " + e.getMessage());

			return new ErrorResponse(req.getToken(), 500, "SPU creation failed: " + req.toString());
		}

		logger.debug("SPU init");

		Response init = spu.init();

		if (init.isError()) {
			logger.debug("SPU initialization failed");
		} else {
			synchronized (subscribeQueue) {
				logger.debug("Add SPU to activation queue");
				subscribeQueue.offer(spu);
				subscribeQueue.notify();
			}
		}

		// return new SubscribeResponse(req.getToken(), spu.getUUID(), req.getAlias(),
		// spu.getFirstResults());
		return init;
	}

	public Response unsubscribe(UnsubscribeRequest req) {
		logger.debug(req);

		SPUManagerBeans.unsubscribeRequest();

		String spuid = req.getSubscribeUUID();

		if (!spus.containsKey(spuid))
			return new ErrorResponse(req.getToken(), 404, "SPUID not found: " + spuid);

		synchronized (unsubscribeQueue) {
			unsubscribeQueue.offer(spus.get(spuid));
			unsubscribeQueue.notify();
		}

		return new UnsubscribeResponse(req.getToken(), spuid);
	}

	public void process(UpdateResponse update) {
		synchronized (updateQueue) {
			logger.debug("Add to update response queue: " + update);
			updateQueue.offer(update);
			updateQueue.notify();
		}
	}

	// SPU processing ended notification
	// @Override
	// public void update(Observable o, Object arg) {
	// SPU spu = (SPU) o;
	//
	// synchronized (processingSpus) {
	// processingSpus.remove(spu);
	// logger.debug("SPUs left: " + processingSpus.size());
	// processingSpus.notify();
	// }
	// }

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

	@Override
	public long getSubscribeRequests() {
		return SPUManagerBeans.getSubscribeRequests();
	}

	@Override
	public long getUnsubscribeRequests() {
		return SPUManagerBeans.getUnsubscribeRequests();
	}

	@Override
	public long getSPUProcessingTimeout() {
		return SPUManagerBeans.getSPUProcessingTimeout();
	}

	@Override
	public void setSPUProcessingTimeout(long t) {
		SPUManagerBeans.setActiveSPUs(t);
	}
}
