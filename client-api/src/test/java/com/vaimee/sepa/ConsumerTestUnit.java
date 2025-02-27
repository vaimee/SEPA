package com.vaimee.sepa;

import java.io.IOException;

import com.vaimee.sepa.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.commons.sparql.BindingsResults;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.pattern.Consumer;

public class ConsumerTestUnit extends Consumer {
	protected static boolean notificationReceived = false;
	protected static boolean firstResultsReceived = false;
	
	private Sync sync;
	
	public ConsumerTestUnit(ConfigurationProvider provider, String subscribeID,Sync sync)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(provider.getJsap(), subscribeID);
		
		this.sync = sync;
	}

	public void syncSubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		Logging.logger.debug("subscribe");
		
		notificationReceived = false;
		firstResultsReceived = false;
		
		super.subscribe(timeout,nretry);
		
		synchronized(this) {
			while (!isSubscribed()) wait();
			Logging.logger.debug("subscribed");
		}
		
		sync.onSubscribe();
	}
	
	public void syncUnsubscribe(long timeout,long nretry) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		Logging.logger.debug("unsubscribe");
		
		super.unsubscribe(timeout,nretry);
		
		synchronized(this) {
			while (isSubscribed()) wait();
			Logging.logger.debug("unsubscribed");
		}
		
		sync.onUnsubscribe();
	}
	
	public void waitNotification() throws InterruptedException {
		synchronized(this) {
			Logging.logger.debug("waitNotification");
			while (!notificationReceived) wait();
			notificationReceived = false;
			Logging.logger.debug("notification received");
		}
	}
	
	public void waitFirstNotification() throws InterruptedException {
		synchronized(this) {
			Logging.logger.debug("waitFirstNotification");
			while (!firstResultsReceived) wait();
			firstResultsReceived = false;
			Logging.logger.debug("notification received");
		}
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		synchronized(this) {
			Logging.logger.debug("onResults");
			notificationReceived = true;
			notify();
			sync.onSemanticEvent();
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
