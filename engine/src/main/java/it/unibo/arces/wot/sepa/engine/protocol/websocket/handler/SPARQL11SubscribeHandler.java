package it.unibo.arces.wot.sepa.engine.protocol.websocket.handler;

import java.nio.channels.NotYetConnectedException;
import java.util.HashSet;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import it.unibo.arces.wot.sepa.commons.request.Request;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Ping;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;
import it.unibo.arces.wot.sepa.engine.scheduling.ResponseAndNotificationListener;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SPARQL11SubscribeHandler extends Thread implements ResponseAndNotificationListener {
	protected Logger logger = LogManager.getLogger("SubscribeHandler");

	private Scheduler scheduler;
	private WebSocket socket;
	private HashSet<String> spuIds = new HashSet<String>();
	private long timeout;

	/*
	 * SPARQL 1.1 Subscribe language
	 * 
	 * {"subscribe":"SPARQL Query 1.1", "authorization": "Bearer JWT",
	 * "alias":"an alias for the subscription"}
	 * 
	 * {"unsubscribe":"SPUID", "authorization": "Bearer JWT"}
	 * 
	 * If security is not required (i.e., ws), authorization key MAY be missing
	 */
	public SPARQL11SubscribeHandler(Scheduler scheduler, WebSocket socket, long timeout)
			throws IllegalArgumentException {
		if (scheduler == null || socket == null)
			throw new IllegalArgumentException("One or more arguments are null");

		this.scheduler = scheduler;
		this.socket = socket;

		if (timeout < 0)
			this.timeout = 5000L;
		else
			this.timeout = timeout;

		this.setName("SEPA Subscribe handler");
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(timeout);
			} catch (InterruptedException e) {
				return;
			}

			try {
				Ping ping = new Ping();
				socket.send(ping.toString());
			} catch (WebsocketNotConnectedException | NotYetConnectedException e) {
				for (String subId : spuIds) {
					logger.debug(">> Scheduling UNSUBSCRIBE request " + subId);
					scheduler.schedule(new UnsubscribeRequest(subId), null);
				}

				// GC
				spuIds = null;
				socket = null;
				scheduler = null;

				return;
			}
		}

	}

	public void processRequest(String message) {
		Request request = null;
		try {
			request = parseRequest(message);
		} catch (Exception e) {
			request = null;
		}
		if (request == null) {
			logger.debug("Not supported request: " + message);
			ErrorResponse response = new ErrorResponse(HttpStatus.SC_BAD_REQUEST, "Not supported request: " + message);
			socket.send(response.toString());
			return;
		}

		logger.debug(">> Scheduling request: " + request.toString());
		scheduler.schedule(request, this);
	}

	protected Request parseRequest(String request)
			throws JsonParseException, JsonSyntaxException, IllegalStateException, ClassCastException {
		JsonObject req;

		req = new JsonParser().parse(request).getAsJsonObject();

		if (req.get("subscribe") != null) {
			String sparql = req.get("subscribe").getAsString();
			if (req.get("alias") != null) {
				String alias = req.get("alias").getAsString();
				return new SubscribeRequest(sparql, alias);
			}
			return new SubscribeRequest(sparql);
		}
		if (req.get("unsubscribe") != null) {
			String spuid = req.get("unsubscribe").getAsString();
			return new UnsubscribeRequest(spuid);
		}

		return null;
	}

	@Override
	public void notify(Response response) {
		if (response.getClass().equals(SubscribeResponse.class)) {
			logger.debug("<< SUBSCRIBE response #" + response.getToken());

			synchronized (spuIds) {
				spuIds.add(((SubscribeResponse) response).getSpuid());
			}

		} else if (response.getClass().equals(UnsubscribeResponse.class)) {
			logger.debug("<< UNSUBSCRIBE response #" + response.getToken() + " ");

			synchronized (spuIds) {
				spuIds.remove(((UnsubscribeResponse) response).getSpuid());
			}
		}

		// Send response to client
		if (socket != null)
			if (socket.isOpen())
				socket.send(response.toString());

	}
}
