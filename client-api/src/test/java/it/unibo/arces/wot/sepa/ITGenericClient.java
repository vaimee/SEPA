package it.unibo.arces.wot.sepa;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ITGenericClient extends GenericClient {
	
	private int notifications;
	private int subscriptions;

	public ITGenericClient(JSAP appProfile, ClientSecurityManager sm, ISubscriptionHandler handler)
			throws SEPAProtocolException {
		super(appProfile, sm, handler);
		
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
}
