package it.unibo.arces.wot.sepa;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.logging.Logging;
import it.unibo.arces.wot.sepa.pattern.Aggregator;

public class AggregatorTestUnit extends Aggregator {
	protected static boolean notificationReceived = false;
	protected static boolean firstResultsReceived = false;
	
	private Sync sync;
	
	public AggregatorTestUnit(ConfigurationProvider provider, String subscribeID, String updateID, Sync sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(provider.getJsap(), subscribeID, updateID);
		
		this.sync = sync;
	}

	@Override
	public void onResults(ARBindingsResults results) {
		synchronized(this) {
			notificationReceived = true;
			notify();
			sync.onSemanticEvent();
		}
		
		try {
			Response ret = update();
			if (ret.isError()) Logging.logger.error(ret);
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			Logging.logger.error(e);
		}
	}
	
	public void syncSubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		Logging.logger.debug("subscribe");
		
		notificationReceived = false;
		firstResultsReceived = false;
		
		super.subscribe(timeout,nretry);
		
		synchronized(this) {
			while (!isSubscribed()) wait();
			Logging.logger.debug("subscribed");
			sync.onSubscribe();
		}	
	}
	
	public void syncUnsubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		Logging.logger.debug("unsubscribe");
		
		super.unsubscribe(timeout,nretry);
		
		synchronized(this) {
			while (isSubscribed()) wait();
			Logging.logger.debug("unsubscribed");
			sync.onUnsubscribe();
		}
	}
	
	public void waitNotification() throws InterruptedException {
		synchronized(this) {
			Logging.logger.debug("waitNotification");
			while (!notificationReceived) wait();
			notificationReceived = false;
			Logging.logger.debug("notify!");
		}
	}
	
	public void waitFirstNotification() throws InterruptedException {
		synchronized(this) {
			Logging.logger.debug("waitFirstNotification");
			while (!firstResultsReceived) wait();
			firstResultsReceived = false;
			Logging.logger.debug("first results received");
		}
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		synchronized(this) {
			Logging.logger.debug("onFirstResults");
			firstResultsReceived = true;
			notify();
		}	
	}
}
