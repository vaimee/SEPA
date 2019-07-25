package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

class SubscriptionManager {
	private static final Logger logger = LogManager.getLogger();

	// Active gates
	private static final HashMap<String, ArrayList<String>> gates = new HashMap<String, ArrayList<String>>();
	
	private static Processor processor = null;

	public static void setProcessor(Processor p) {
		processor = p;
	}

	public static synchronized void onSubscribe(String gid, String sid) {
		if (gid == null) {
			logger.error("@onSubscribe GID is null");
			return;
		}
		if (sid == null) {
			logger.error("@onSubscribe SID is null");
			return;
		}

		// Gate exists?
		if (!gates.containsKey(gid)) {
			// Create a new gate entry
			logger.debug("@onSubscribe create new gate: " + gid);
			gates.put(gid, new ArrayList<String>());
		}

		// Add subscription
		gates.get(gid).add(sid);
		
		logger.trace("ADDED " + gid + " " + sid + " " + gates.size() + " " + gates.get(gid).size());
	}

	public static synchronized void onUnsubscribe(String gid, String sid) {
		if (gid == null) {
			logger.error("@onUnsubscribe GID is null");
			return;
		}
		if (sid == null) {
			logger.error("@onUnsubscribe SID is null");
			return;
		}

		logger.trace("REMOVE " + gid + " " + sid + " " + gates.size() + " " + gates.get(gid).size());

		// Remove subscription
		gates.get(gid).remove(sid);
	}

	public static synchronized void onClose(String gid) throws SEPAProcessingException {
		if (gid == null) {
			logger.error("@onClose GID is null");
			return;
		}
		if (processor == null) {
			logger.error("@onClose processor is null");
			return;
		}

		// Gate exists?
		if (!gates.containsKey(gid)) {
			logger.warn("NOT_FOUND " + gid + " " + "---" + " " + gates.size() + " " + "-1");
			return;
		}

		logger.trace("CLOSE " + gid + " --- " + gates.size() + " " + gates.get(gid).size());

		// Kill all active subscriptions
		for (String sid : gates.get(gid)) {
			processor.killSubscription(sid, gid);
		}

		// Remove gate
		gates.remove(gid);
	}

	public static synchronized void onError(String gid, Exception e) {
		if (gid == null) {
			logger.error("@onError GID is null");
			return;
		}

		logger.error("@onError GID:" + gid + " Exception:" + e);
	}

	public static long getNumberOfGates() {
		return gates.size();
		
	}
}
