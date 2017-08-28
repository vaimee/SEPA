package it.unibo.arces.wot.sepa.engine.processing;

import it.unibo.arces.wot.sepa.commons.response.Notification;

public interface SPUListener {
	public void SPUNotify(Notification notify);
}
