package it.unibo.arces.wot.sepa.engine.core;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public interface EventHandler {
	public void notifyEvent(Notification notify) throws SEPAProtocolException;
}
