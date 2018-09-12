package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerQueue;

public class DependabilityManager {
	private static final Logger logger = LogManager.getLogger();

	// Active subscriptions
	private static final HashMap<UUID, ArrayList<String>> subscriptions = new HashMap<UUID, ArrayList<String>>();

	// Scheduler queue
	private final SchedulerQueue schedulerQueue;

	public DependabilityManager(SchedulerQueue schedulerQueue) {
		this.schedulerQueue = schedulerQueue;
	}

	public synchronized void onSubscribe(UUID uuid, String spuid) {
		if (uuid == null || spuid == null) {
			logger.error("Some values are null. UUID: "+uuid+" SPUID: "+spuid);
			return;
		}
		
		if (!subscriptions.containsKey(uuid))
			subscriptions.put(uuid, new ArrayList<String>());
		subscriptions.get(uuid).add(spuid);
		
		logger.debug("@onSubscribe Subscriptions: " + subscriptions.size()+" Handlers (" + uuid + "): " + subscriptions.get(uuid).size());
	}

	public synchronized void onUnsubscribe(UUID uuid, String spuid) {
		logger.debug("@onUnsubscribe: " + uuid + " SPUID: " + spuid);

		subscriptions.get(uuid).remove(spuid);
		if (subscriptions.get(uuid).isEmpty())
			subscriptions.remove(uuid);
		
		logger.debug("Active subscriptions: "+subscriptions.size());
	}

	public synchronized void onBrokenSubscription(UUID uuid) {
		if (!subscriptions.containsKey(uuid)) {
			logger.warn("Broken socket not registered: "+uuid);
			return;
		}

		logger.debug(String.format("@onBrokenSocket: " + uuid +" with active subscriptions: %d", subscriptions.get(uuid).size())+" (sockets opened: "+subscriptions.size()+")");

		// Kill all SPUs
		for (String spuid : subscriptions.get(uuid)) {
			logger.trace("Schedule request to kill SPU: " + spuid);
			schedulerQueue.killSpuid(spuid);
		}

		// Remove subscriptions
		subscriptions.remove(uuid);
	}

	public void onError(UUID uuid, ErrorResponse error) {
		logger.error("Subscription:" + uuid + " error:" + error);
	}
}
