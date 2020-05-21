package it.unibo.arces.wot.sepa.pattern;

import java.io.IOException;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;

public class ITGenericClient{	
	
	private int notifications;
	private int subscriptions;
	
	private GenericClient client;

	public ITGenericClient(JSAP appProfile, ClientSecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		client = new GenericClient(appProfile, sm, handler);
		
		notifications = 0;
		subscriptions = 0;
	}

	public void setOnSemanticEvent(String spuid) {
		notifications++;
	}
	
	public int getNotificationsCount() {
		return notifications;
	}

	public void setOnSubscribe(String spuid, String alias) {
		subscriptions++;
	}
	
	public int getSubscriptionsCount() {
		return subscriptions;
	}

	public void setOnUnsubscribe(String spuid) {
		subscriptions--;
	}

	public void subscribe(String ID, Bindings forced, int timeout, String alias) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException {
		client.subscribe(ID, forced, timeout, alias);
		
	}

	public void update(String ID, Bindings forced, int timeout) throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException, SEPABindingsException {
		client.update(ID, forced, timeout);
		
	}

	public void unsubscribe(String subID, int timeout) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		client.unsubscribe(subID, timeout);
	}
}
