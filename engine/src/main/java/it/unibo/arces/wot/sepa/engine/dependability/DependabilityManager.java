package it.unibo.arces.wot.sepa.engine.dependability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

public class DependabilityManager {
	private static final Logger logger = LogManager.getLogger();

	// Active sockets
	private HashMap<WebSocket, ArrayList<String>> subscriptions = new HashMap<WebSocket, ArrayList<String>>();

	// Broken sockets
	private ArrayList<WebSocket> brokenSockets = new ArrayList<WebSocket>();

	private final BlockingQueue<String> killSpuids;
	
	public DependabilityManager(BlockingQueue<String> spuids) {
		this.killSpuids = spuids;
	}

	public void onSubscribe(WebSocket conn, String spuid) {
		logger.debug("@onSubscribe: "+conn+" SPUID: "+spuid);
		
		if (brokenSockets.contains(conn)) {
			logger.debug("Socket has been closed: "+conn+" SPUID: "+spuid);
			logger.debug("Request to kill SPU: "+spuid);
			try {
				killSpuids.put(spuid);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
			}
			return;
		}
		
		if (!subscriptions.containsKey(conn))
			subscriptions.put(conn, new ArrayList<String>());
		subscriptions.get(conn).add(spuid);
	}

	public void onUnsubscribe(WebSocket conn, String spuid) {
		logger.debug("@onUnsubscribe: "+conn+" SPUID: "+spuid);
		
		if (subscriptions.containsKey(conn)) {
			subscriptions.get(conn).remove(spuid);
			if (subscriptions.get(conn).isEmpty())
				subscriptions.remove(conn);
		}
	}

	public void onBrokenSocket(WebSocket conn) {
		logger.debug("@onBrokenSocket: "+conn);
		
		if (!subscriptions.containsKey(conn)) {
			brokenSockets.add(conn);
			return;
		}
		
		logger.debug(String.format("Broken socket with %d active subscriptions", subscriptions.get(conn).size()));

		// Kill all SPUs
		for (String spuid : subscriptions.get(conn)) {
			logger.debug("Request to kill SPU: "+spuid);
			try {
				killSpuids.put(spuid);
			} catch (InterruptedException e) {
				logger.warn(e.getMessage());
			}
		}
		
		// Remove subscriptions
		subscriptions.remove(conn);
	}
}
