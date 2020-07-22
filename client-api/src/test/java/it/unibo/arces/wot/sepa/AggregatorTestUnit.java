package it.unibo.arces.wot.sepa;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class AggregatorTestUnit extends Aggregator {
	protected static boolean notificationReceived = false;
	protected static boolean firstResultsReceived = false;
	
	public AggregatorTestUnit(JSAP appProfile, String subscribeID, String updateID, ClientSecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException {
		super(appProfile, subscribeID, updateID, sm);
	}

	@Override
	public void onResults(ARBindingsResults results) {
		synchronized(this) {
			notificationReceived = true;
			notify();
		}
		
		try {
			Response ret = update();
			if (ret.isError()) logger.error(ret);
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(e);
		}
		
	}
	
	public void syncSubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		logger.debug("subscribe");
		
		notificationReceived = false;
		firstResultsReceived = false;
		
		super.subscribe(timeout,nretry);
		
		synchronized(this) {
			while (!isSubscribed()) wait();
			logger.debug("subscribed");
		}
	}
	
	public void syncUnsubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		logger.debug("unsubscribe");
		
		super.unsubscribe(timeout,nretry);
		
		synchronized(this) {
			while (isSubscribed()) wait();
			logger.debug("unsubscribed");
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
	
	public void waitFirstNotification() throws InterruptedException {
		synchronized(this) {
			logger.debug("waitFirstNotification");
			while (!firstResultsReceived) wait();
			firstResultsReceived = false;
			logger.debug("first results received");
		}
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		synchronized(this) {
			logger.debug("onFirstResults");
			firstResultsReceived = true;
			notify();
		}	
	}
}
