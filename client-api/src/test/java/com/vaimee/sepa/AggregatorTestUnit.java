package com.vaimee.sepa;

import java.io.IOException;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.logging.Logging;
import com.vaimee.sepa.api.pattern.Aggregator;

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
			if (ret.isError()) Logging.getLogger().error(ret);
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			Logging.getLogger().error(e);
		}
	}
	
	public void syncSubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		Logging.getLogger().debug("subscribe");
		
		notificationReceived = false;
		firstResultsReceived = false;
		
		super.subscribe(timeout,nretry);
		
		synchronized(this) {
			while (!isSubscribed()) wait();
			Logging.getLogger().debug("subscribed");
			sync.onSubscribe();
		}	
	}
	
	public void syncUnsubscribe(long timeout,long nretry) throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		Logging.getLogger().debug("unsubscribe");
		
		super.unsubscribe(timeout,nretry);
		
		synchronized(this) {
			while (isSubscribed()) wait();
			Logging.getLogger().debug("unsubscribed");
			sync.onUnsubscribe();
		}
	}
	
	public void waitNotification() throws InterruptedException {
		synchronized(this) {
			Logging.getLogger().debug("waitNotification");
			while (!notificationReceived) wait();
			notificationReceived = false;
			Logging.getLogger().debug("notify!");
		}
	}
	
	public void waitFirstNotification() throws InterruptedException {
		synchronized(this) {
			Logging.getLogger().debug("waitFirstNotification");
			while (!firstResultsReceived) wait();
			firstResultsReceived = false;
			Logging.getLogger().debug("first results received");
		}
	}

	@Override
	public void onFirstResults(BindingsResults results) {
		synchronized(this) {
			Logging.getLogger().debug("onFirstResults");
			firstResultsReceived = true;
			notify();
		}	
	}
}
