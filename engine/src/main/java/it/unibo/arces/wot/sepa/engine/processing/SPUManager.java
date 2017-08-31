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
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UpdateResponse;
import it.unibo.arces.wot.sepa.engine.bean.EngineBeans;
import it.unibo.arces.wot.sepa.engine.bean.SEPABeans;
import it.unibo.arces.wot.sepa.engine.bean.SPUManagerBeans;
import it.unibo.arces.wot.sepa.engine.scheduling.ScheduledRequest;

public class SPUManager implements Observer, SPUManagerMBean {
	private static final Logger logger = LogManager.getLogger("SPUManager");

	private SPARQL11Protocol endpoint;

	// SPUs and SPUIDs hash mapx
	private HashMap<String, SPU> spus = null;

	// Sequential update processing
	private static int subscriptionsChecked = 0;

	private Keepalive ping = null;

	public SPUManager(SPARQL11Protocol endpoint) {
		this.endpoint = endpoint;
		SEPABeans.registerMBean("SEPA:type=" + this.getClass().getSimpleName(), this);

		spus = new HashMap<String, SPU>();

		ping = new Keepalive();
		ping.setName("SEPA Keepalive");
		ping.start();
	}

	class Keepalive extends Thread {
		HashSet<SPU> toBeTerminated = new HashSet<SPU>();

		@Override
		public void run() {

			while (true) {
				try {
					Thread.sleep(EngineBeans.getKeepalive());
				} catch (InterruptedException e) {
					return;
				}

				// Send ping to check broken sockets
				synchronized (spus) {
					toBeTerminated.clear();
					for (SPU spu : spus.values()) {
						if (spu.sendPing())
							continue;
						toBeTerminated.add(spu);
					}

					// Terminate SPUs
					for (SPU spu : toBeTerminated) {
						spus.remove(spu.getUUID());
						spu.terminate();
					}
				}
				SPUManagerBeans.setActiveSPUs(spus.size());
			}
		}
	}

	public synchronized Response processSubscribe(ScheduledRequest req) {
		logger.debug("Process SUBSCRIBE #" + req.getToken());

		if (!req.getRequest().getClass().equals(SubscribeRequest.class))
			return new ErrorResponse(req.getToken(), 400, "Bad request: " + req.getRequest().toString());

		// TODO: choose different kinds of SPU based on subscription request
		SPU spu = new SPUNaive(req, endpoint);
		if (spu.init()) {

			spu.addObserver(this);

			// Add new SPU to the list of active SPUs
			synchronized (spus) {
				spus.put(spu.getUUID(), spu);
				SPUManagerBeans.setActiveSPUs(spus.size());
			}

			// Start the SPU thread
			Thread th = new Thread(spu);
			th.setName("SPU_" + spu.getUUID());
			th.start();

			return new SubscribeResponse(req.getToken(), spu.getUUID(),
					((SubscribeRequest) req.getRequest()).getAlias(),spu.getFirstResults());
		}
		else return new ErrorResponse(req.getToken(), 400, "Bad request: " + req.getRequest().toString());
	}

	public synchronized Response processUnsubscribe(ScheduledRequest req) {
		logger.debug("Process UNSUBSCRIBE #" + req.getToken());

		if (!req.getRequest().getClass().equals(UnsubscribeRequest.class))
			return new ErrorResponse(req.getToken(), 400, "Bad unsubscribe request: " + req.getRequest().toString());

		String spuid = ((UnsubscribeRequest) req.getRequest()).getSubscribeUUID();

		synchronized (spus) {
			if (spus.containsKey(spuid)) {
				spus.get(spuid).terminate();
				spus.remove(spuid);
				SPUManagerBeans.setActiveSPUs(spus.size());

			} else
				return new ErrorResponse(req.getToken(), 404, "Not found: " + spuid);
		}

		return new UnsubscribeResponse(req.getToken(), spuid);
	}

	public synchronized void processUpdate(UpdateResponse res) {
		logger.debug("*** PROCESSING UPDATE STARTED ***");

		// Wait all SPUs completing processing
		Instant start = Instant.now();
		waitAllSubscriptionChecks(res);
		Instant stop = Instant.now();

		SPUManagerBeans.timings(start, stop);

		logger.debug("*** PROCESSING UPDATE FINISHED ***");
	}

	private synchronized void waitAllSubscriptionChecks(UpdateResponse res) {
		subscriptionsChecked = 0;

		// Wake-up all SPUs
		logger.debug("Activate SPUs (Total: " + spus.size() + ")");
		synchronized (spus) {
			for (SPU spu : spus.values())
				spu.subscriptionCheck(res);

		}

		logger.debug("Waiting all SPUs to complete processing...");
		while (subscriptionsChecked != spus.size()) {
			try {
				wait();
			} catch (InterruptedException e) {
				logger.debug("SPUs processing ended " + subscriptionsChecked + "/" + spus.size());
			}
		}

	}

	private synchronized void subscriptionProcessingEnded(String spuid) {
		subscriptionsChecked++;
		notify();
		logger.debug("Checked subscription " + spuid + " (" + subscriptionsChecked + "/" + spus.size() + ")");
	}

	@Override
	public void update(Observable o, Object arg) {
		Notification ret = (Notification) arg;

		// SPU processing ended
		logger.debug("SPU " + ret.getSpuid() + " processing ended");
		subscriptionProcessingEnded(ret.getSpuid());
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
	public String getSPUs_statistics() {
		return SPUManagerBeans.getSPUs_statistics();
	}

	@Override
	public void reset() {
		SPUManagerBeans.reset();
	}
}
