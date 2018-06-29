package it.unibo.arces.wot.sepa.apps.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class BasicHandler implements ISubscriptionHandler {
	private static final Logger logger = LogManager.getLogger();
	
	@Override
	public void onSemanticEvent(Notification notify) {
		logger.info("Notify: "+notify);
	}

	@Override
	public void onBrokenConnection() {
		logger.info("Broken socket");
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.info("Error: "+errorResponse);
	}

}
