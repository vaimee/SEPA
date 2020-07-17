package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;

import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;
import it.unibo.arces.wot.sepa.api.SubscriptionProtocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class Subscriber extends Thread implements Closeable {
	protected final Logger logger = LogManager.getLogger();

	private final SPARQL11SEProtocol client;
	private final String id;

	private static ConfigurationProvider provider;

	public Subscriber(String id, ISubscriptionHandler sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		
		this.setName("Subscriber-"+id+"-"+this.getId());
		
		provider = new ConfigurationProvider();
		
		this.id = id;
		
		SubscriptionProtocol protocol = new WebsocketSubscriptionProtocol(provider.getJsap().getSubscribeHost(),
					provider.getJsap().getSubscribePort(), provider.getJsap().getSubscribePath(),sync,provider.getSecurityManager());

		client = new SPARQL11SEProtocol(protocol);
	}

	public void run() {
		if(provider.getJsap().isSecure()){
			try {
				provider.getSecurityManager().register("SEPATest");
			} catch (SEPASecurityException | SEPAPropertiesException  e) {
				logger.error(e);
			}
		}
		synchronized (this) {
			try {
				logger.debug("subscribe");
				client.subscribe(provider.buildSubscribeRequest(id));
				logger.debug("wait");
				wait();
			} catch (SEPAProtocolException | InterruptedException e) {
				try {
					client.close();
				} catch (IOException e1) {
					logger.error(e1.getMessage());
				}
				return;
			}
		}
	}
	
//	public void unsubscribe(String id) {
//		try {
//			client.unsubscribe(provider.buildUnsubscribeRequest(id));
//		} catch (SEPAProtocolException e) {
//			logger.error(e.getMessage());
//		}	
//	}

	public void close() {
		logger.debug("close");
		try {
			client.unsubscribe(provider.buildUnsubscribeRequest(id));
		} catch (SEPAProtocolException e) {
			logger.error(e.getMessage());
		}
		interrupt();
	}
}
