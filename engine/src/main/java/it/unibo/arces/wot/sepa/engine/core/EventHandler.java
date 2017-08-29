package it.unibo.arces.wot.sepa.engine.core;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Ping;

public interface EventHandler extends ResponseHandler {
	public void notifyEvent(Notification notify);
	public void sendPing(Ping ping) throws IOException;
}
