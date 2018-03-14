package it.unibo.arces.wot.sepa.engine.dependability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.core.ResponseHandler;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class DependabilityManager implements ResponseHandler {
	private static final Logger logger = LogManager.getLogger("DependabilityManager");
	
	// Active sockets
	private HashMap<WebSocket,ArrayList<String>> subscriptions = new HashMap<WebSocket,ArrayList<String>>();
	
	private ArrayList<String> unsubscribeList = new ArrayList<String>();
	
	private Scheduler scheduler;
	
	public DependabilityManager(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	public void onSubscribe(WebSocket conn,String spuid) {
		if (!subscriptions.containsKey(conn)) subscriptions.put(conn, new ArrayList<String>());	
		subscriptions.get(conn).add(spuid);
	}
	
	public void onUnsubscribe(WebSocket conn,String spuid) {
		subscriptions.get(conn).remove(spuid);
		if (subscriptions.get(conn).isEmpty()) subscriptions.remove(conn);
	}
	
	public void onBrokenSocket(WebSocket conn) {
		logger.info(String.format("Broken socket with %d active subscriptions", subscriptions.get(conn).size()));
		synchronized(unsubscribeList) {
			unsubscribeList.addAll(subscriptions.get(conn));
			logger.info(String.format("Pending unsubscribe requests: %d", unsubscribeList.size()));
		}
		for(String spuid: subscriptions.get(conn)) {
			scheduler.schedule(new UnsubscribeRequest(spuid), this);	
		}	
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		if (response.isUnsubscribeResponse()) {
			UnsubscribeResponse ret = (UnsubscribeResponse) response;
			String spuid = ret.getSpuid();
			synchronized(unsubscribeList) {
				unsubscribeList.remove(spuid);
				logger.info(String.format("Pending unsubscribe requests: %d", unsubscribeList.size()));
			}
		}
		
	}
}
