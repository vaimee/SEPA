package it.unibo.arces.wot.sepa.pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.INotificationHandler;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class DefaultNotificationHandler implements INotificationHandler {
	private final Logger logger = LogManager.getLogger("DefaultNotificationHandler");
	
	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug("onSemanticEvent "+notify.toString());
	}

	@Override
	public void onSubscribeConfirm(SubscribeResponse response) {
		logger.debug("onSubscribeConfirm "+response.getSpuid()+ " alias: "+response.getAlias());
	}

	@Override
	public void onUnsubscribeConfirm(UnsubscribeResponse response) {
		logger.debug("onUnsubscribeConfirm "+response.getSpuid());
	}

	@Override
	public void onPing() {
		logger.debug("onPing");
		
	}

	@Override
	public void onBrokenSubscription() {
		logger.debug("onBrokenSubscription");
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.debug("onError "+errorResponse.toString());
		
	}	
}
