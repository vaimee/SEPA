package it.unibo.arces.wot.sepa.engine.core;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.response.Notification;

public interface EventHandler extends ResponseHandler {
	public void notifyEvent(Notification notify) throws IOException;
}
