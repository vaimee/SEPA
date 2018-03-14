package it.unibo.arces.wot.sepa.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

public class SPARQL11SEWebsocket {
	private Logger logger = LogManager.getLogger("SPARQL11SEWebsocket");

	private long TIMEOUT = 5000;

	protected SEPAWebsocketClient client = null;
	
	public SPARQL11SEWebsocket(String wsUrl, ISubscriptionHandler handler) throws SEPAProtocolException {
		try {
			if (handler == null) {
				logger.fatal("Notification handler is null. Client cannot be initialized");
				throw new SEPAProtocolException(new IllegalArgumentException("Notificaton handler is null"));
			}
			client = new SEPAWebsocketClient(new URI(wsUrl), handler);
		} catch (URISyntaxException e) {
			throw new SEPAProtocolException(e);
		}
	}
	
	public Response subscribe(String sparql,String alias) {
		return secureSubscribe(sparql,null,alias);
	}
	
	public Response subscribe(String sparql) {
		return secureSubscribe(sparql,null,null);
	}

	public Response secureSubscribe(String sparql,String authorization) {
		return secureSubscribe(sparql,authorization,null);
	}
	
	public Response secureSubscribe(String sparql,String authorization,String alias) {
		if (sparql == null)
			return new ErrorResponse(500, "SPARQL query is null");

		// Create SPARQL 1.1 Subscribe request
		JsonObject body = new JsonObject();
		JsonObject request = new JsonObject();
		body.add("sparql", new JsonPrimitive(sparql));
		if (authorization != null) body.add("authorization", new JsonPrimitive(authorization));
		if (alias != null) body.add("alias", new JsonPrimitive(alias));
		request.add("subscribe", body);
		
		if (!client.isOpen()) {
			try {
				if(!client.connectBlocking()) {
					logger.error("Not connected");
					return new ErrorResponse(500,"Not connected");
				}
			} catch (InterruptedException e) {
				logger.debug(e);
				return new ErrorResponse(500,"Not connected");
			}
		}
		
		// Send request and wait for response
		Response ret = client.sendAndReceive(request.toString(), TIMEOUT);
		
		//if (ret.isSubscribeResponse()) connected = true;
		
		return ret;
	}
	
	public Response unsubscribe(String spuid) {
		return unsubscribe(spuid,null);
	}

	public Response unsubscribe(String spuid,String authorization) {
		if (spuid == null)
			return new ErrorResponse(500, "SPUID is null");

		// Create SPARQL 1.1 Unsubscribe request
		JsonObject request = new JsonObject();
		JsonObject body = new JsonObject();
		body.add("spuid", new JsonPrimitive(spuid));
		if (authorization != null) body.add("authorization", new JsonPrimitive(authorization));
		request.add("unsubscribe", body);
		
		if (client.isClosed()) return new ErrorResponse(500,"Socket is closed");
		
//		if (!connected) {
//			client = new SEPAWebsocketClient(wsURI, this);
//			try {
//				if(!client.connectBlocking()) {
//					logger.error("Not connected");
//					return new ErrorResponse(500,"Not connected");
//				}
//			} catch (InterruptedException e) {
//				return new ErrorResponse(500,"Not connected");
//			}
//		}
		
		// Send request and wait for response
		return client.sendAndReceive(request.toString(), TIMEOUT);
	}

//	@Override
//	public void onSemanticEvent(Notification notify) {
//		handler.onSemanticEvent(notify);
//	}
//
//	@Override
//	public void onPing() {
//		handler.onPing();
//	}
//
//	@Override
//	public void onBrokenSocket() {	
//		if(connected) {
//			connected = false;
//			handler.onBrokenSocket();	
//		}
//	}
//
//	@Override
//	public void onError(ErrorResponse errorResponse) {
//		handler.onError(errorResponse);
//	}

	public void close() {
		if (client.isOpen()){
				client.close();
		}
	}

}
