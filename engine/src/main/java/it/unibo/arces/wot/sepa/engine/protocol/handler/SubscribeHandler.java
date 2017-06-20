package it.unibo.arces.wot.sepa.engine.protocol.handler;

import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;

import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SubscribeHandler extends Thread {
	protected Logger logger = LogManager.getLogger("SubscribeHandler");
	private Scheduler scheduler;
	private WebSocket socket;
	private String text;
	private HashMap<WebSocket, ResponseAndNotificationListener> activeSockets;
	
	/* SPARQL 1.1 Subscribe language 
	 * 
	 * {"subscribe":"SPARQL Query 1.1", "authorization": "Bearer JWT", "alias":"an alias for the subscription"}
	 * 
	 * {"unsubscribe":"SPUID", "authorization": "Bearer JWT"}
	 * 
	 * If security is not required (i.e., ws), authorization key MAY be missing
	 * */
	public SubscribeHandler(Scheduler scheduler,WebSocket socket,String text,HashMap<WebSocket, ResponseAndNotificationListener> activeSockets) throws IllegalArgumentException {
		this.scheduler = scheduler;
		this.socket = socket;
		this.text = text;
		this.activeSockets = activeSockets;
		
		if (scheduler == null || socket == null || text == null || activeSockets == null) throw new IllegalArgumentException("One or more arguments are null");
	}
	public void run() {
		int token = scheduler.getToken();
		if (token == -1) {
			ErrorResponse response = new ErrorResponse(token, HttpStatus.SC_SERVICE_UNAVAILABLE, "No more tokens");
			socket.send(response.toString());
			return;
		}

		Request request = null;
		try {
			request = parseRequest(token, text);
		} catch (Exception e) {
			request = null;
		}
		if (request == null) {
			logger.debug("Not supported request: " + text);
			ErrorResponse response = new ErrorResponse(token, HttpStatus.SC_BAD_REQUEST, "Not supported request: "+text);
			socket.send(response.toString());

			scheduler.releaseToken(token);
			return;
		}

		synchronized (activeSockets) {
			logger.debug(">> Scheduling request: " + request.toString());
			scheduler.addRequest(request, activeSockets.get(socket));
		}
	}
	
	protected Request parseRequest(Integer token, String request)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;

		req = new JsonParser().parse(request).getAsJsonObject();

		if (req.get("subscribe") != null) {
			String sparql = req.get("subscribe").getAsString();
			if (req.get("alias") != null) {
				String alias = req.get("alias").getAsString();
				return new SubscribeRequest(token, sparql, alias);
			}
			return new SubscribeRequest(token, sparql);
		}
		if (req.get("unsubscribe") != null) {
			String spuid = req.get("unsubscribe").getAsString();
			return new UnsubscribeRequest(token, spuid);
		}

		return null;
	}
}
