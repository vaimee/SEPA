package it.unibo.arces.wot.sepa.pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public class ITAggregator extends Aggregator {
	protected final Logger logger = LogManager.getLogger();
	
	protected static boolean subscribed = false;
	protected static boolean notificationReceived = false;
	
	public ITAggregator(JSAP appProfile, String subscribeID, String updateID, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, subscribeID, updateID, sm);
	}
	
	public void subscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		super.subscribe(5000);
		synchronized(this) {
			while (!subscribed) wait();
		}
	}
	
	public void waitNotification() throws InterruptedException {
		synchronized(this) {
			while (!notificationReceived) wait();
			notificationReceived = false;
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
		synchronized(this) {
			notificationReceived = true;
			notify();
		}
		
		try {
			update();
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(e);
		}
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBrokenConnection() {
		subscribed = false;
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		synchronized(this) {
			subscribed = true;
			notify();
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		subscribed = false;
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		synchronized(this) {
			notificationReceived = true;
			notify();
		}
	}
}
