package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerQueue;

public class DependabilityManager {
	private static final Logger logger = LogManager.getLogger();

	// Active subscriptions
	private static final HashMap<Integer, ArrayList<String>> subscriptions = new HashMap<Integer, ArrayList<String>>();

	// Scheduler queue
	private final SchedulerQueue schedulerQueue;

	public DependabilityManager(SchedulerQueue schedulerQueue) {
		this.schedulerQueue = schedulerQueue;
	}

	public synchronized void onSubscribe(Integer hash, String spuid) {
		logger.debug("@onSubscribe: " + hash + " SPUID: " + spuid);

		if (!subscriptions.containsKey(hash))
			subscriptions.put(hash, new ArrayList<String>());
		subscriptions.get(hash).add(spuid);
		
		logger.debug("Active subscriptions: "+subscriptions.size());
	}

	public synchronized void onUnsubscribe(Integer hash, String spuid) {
		logger.debug("@onUnsubscribe: " + hash + " SPUID: " + spuid);

		subscriptions.get(hash).remove(spuid);
		if (subscriptions.get(hash).isEmpty())
			subscriptions.remove(hash);
		
		logger.debug("Active subscriptions: "+subscriptions.size());
	}

	public synchronized void onBrokenSocket(Integer hash) {
		logger.debug("@onBrokenSocket: " + hash);

		if (!subscriptions.containsKey(hash)) return;

		logger.debug(String.format("Broken socket with active subscriptions: %d", subscriptions.get(hash).size()));

		// Kill all SPUs
		for (String spuid : subscriptions.get(hash)) {
			logger.debug("Schedule request to kill SPU: " + spuid);
			schedulerQueue.killSpuid(spuid);
		}

		// Remove subscriptions
		subscriptions.remove(hash);
		
		logger.debug("Active subscriptions: "+subscriptions.size());

	}

	public void onError(Integer hash, ErrorResponse error) {
		logger.error("Subscription:" + hash + " error:" + error);
	}
}
