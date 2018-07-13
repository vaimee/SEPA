package it.unibo.arces.wot.sepa.api;

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import it.unibo.arces.wot.sepa.api.protocol.websocket.WebSocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Subscriber extends Thread implements ISubscriptionHandler {
	
	private JSAP properties;
	private SEPASecurityManager sm;
	private AtomicLong mutex;
	private SPARQL11SEProtocol client = null;
	private ISubscriptionProtocol protocol = null;
	private String id;
	
	private AtomicBoolean running = new AtomicBoolean(true);
	
	public Subscriber(String id,JSAP properties, SEPASecurityManager sm,AtomicLong mutex) {
		this.properties = properties;
		this.sm = sm;
		this.mutex = mutex;
		this.id = id;
		
		try {
			protocol = new WebSocketSubscriptionProtocol(properties.getDefaultHost(),properties.getSubscribePort(),properties.getSubscribePath());
		} catch (SEPAProtocolException e2) {
			assertFalse(e2.getMessage(),true);
		}
		
		if (sm != null)
			try {
				client = new SPARQL11SEProtocol(protocol, this, sm);
			} catch (SEPAProtocolException e1) {
				assertFalse(e1.getMessage(), true);
			}
		else
			try {
				client = new SPARQL11SEProtocol(protocol, this);
			} catch (IllegalArgumentException | SEPAProtocolException e1) {
				assertFalse(e1.getMessage(), true);
			}
	}
	
	public void run() {		
		Response ret = null;
		try {
			ret = client.subscribe(buildSubscribeRequest(id));
		} catch (SEPAPropertiesException | SEPASecurityException e1) {
			assertFalse(e1.getMessage(), true);
		}
		assertFalse(String.valueOf(ret), ret.isError());

		while(running.get()) {
			synchronized(running) {
				try {
					running.wait();
				} catch (InterruptedException e) {
					running.set(false);
				}
			}
		}
		
		try {
			client.close();
		} catch (IOException e) {
			assertFalse(e.getMessage(), true);
		}
	}
	
	public void interrupt() {
		super.interrupt();
		
		synchronized(running) {
			running.set(true);
			running.notify();
		}
	}
	
	private SubscribeRequest buildSubscribeRequest(String id) throws SEPAPropertiesException, SEPASecurityException {
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);
		
		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();

		return new SubscribeRequest(sparql, null, graphUri, namedGraphUri, authorization);
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		synchronized (mutex) {
			mutex.set(mutex.get()-1);
			mutex.notify();
		}	
	}

	@Override
	public void onBrokenConnection() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub
		
	}
}
