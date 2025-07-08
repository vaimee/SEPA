package com.vaimee.sepa.apps.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;

public class BasicHandler implements ISubscriptionHandler {
	private static final Logger logger = LogManager.getLogger();
	
	@Override
	public void onSemanticEvent(Notification notify) {
		logger.info("onSemanticEvent: "+notify);
	}

	@Override
	public void onBrokenConnection(ErrorResponse err) {
		logger.error("onBrokenConnection "+err);
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("onError: "+errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.info("onSubscribe: "+spuid + " Alias: "+alias);
		
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.info("onUnsubscribe: "+spuid);
	}

}
