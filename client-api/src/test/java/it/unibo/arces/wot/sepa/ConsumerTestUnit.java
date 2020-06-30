package it.unibo.arces.wot.sepa;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ConsumerTestUnit extends Consumer {
	protected static boolean notificationReceived = false;
	
	public ConsumerTestUnit(JSAP appProfile, String subscribeID, ClientSecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, subscribeID, sm);
	}

	public void subscribe() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		logger.debug("subscribe");
		
		super.subscribe(5000);
		
		synchronized(this) {
			while (!isSubscribed()) wait();
			logger.debug("subscribed");
		}
	}
	
	public void waitNotification() throws InterruptedException {
		synchronized(this) {
			logger.debug("waitNotification");
			while (!notificationReceived) wait();
			notificationReceived = false;
			logger.debug("notify!");
		}
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		synchronized(this) {
			logger.debug("onResults");
			notificationReceived = true;
			notify();
		}
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		synchronized(this) {
			logger.debug("onFirstResults");
			notificationReceived = true;
			notify();
		}	
	}

}
