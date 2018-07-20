package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicBoolean;

import it.unibo.arces.wot.sepa.api.protocols.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Subscriber extends Thread {

	private JSAP properties;
	private SEPASecurityManager sm;
	private SPARQL11SEProtocol client = null;
	private SubscriptionProtocol protocol = null;
	private String id;

	private AtomicBoolean running = new AtomicBoolean(true);

	public Subscriber(String id, JSAP properties, SEPASecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		this.properties = properties;
		this.sm = sm;
		this.id = id;

		if (sm != null) {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), sm, handler);
			client = new SPARQL11SEProtocol(protocol, sm);
		} else {
			protocol = new WebsocketSubscriptionProtocol(properties.getDefaultHost(), properties.getSubscribePort(),
					properties.getSubscribePath(), handler);
			client = new SPARQL11SEProtocol(protocol);
		}
	}

	public void run() {
		try {
			client.subscribe(buildSubscribeRequest(id, 5000));
		} catch (SEPAPropertiesException | SEPASecurityException | SEPAProtocolException e1) {
			return;
		}

		while (running.get()) {
			synchronized (running) {
				try {
					running.wait();
				} catch (InterruptedException e) {
					running.set(false);
				}
			}
		}

		client.close();
	}

	public void finish() {
		synchronized (running) {
			running.set(false);
			running.notify();
		}
	}

	private SubscribeRequest buildSubscribeRequest(String id, long timeout)
			throws SEPAPropertiesException, SEPASecurityException {
		String sparql = properties.getSPARQLQuery(id);
		String graphUri = properties.getDefaultGraphURI(id);
		String namedGraphUri = properties.getNamedGraphURI(id);

		String authorization = null;
		if (sm != null)
			authorization = sm.getAuthorizationHeader();

		return new SubscribeRequest(sparql, id, graphUri, namedGraphUri, authorization, timeout);
	}
}
