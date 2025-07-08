package com.vaimee.sepa.apps.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.pattern.Aggregator;

/**
 * This abstract class provides a default management of a subscription. If the
 * socket gets broken, the client tries to subscribe again.
 * 
 */
public abstract class ChatAggregator extends Aggregator {
	protected static final Logger logger = LogManager.getLogger();

	public ChatAggregator(String subscribeID, String updateID)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAPProvider().getJsap(), subscribeID, updateID);
	}

	public void joinChat() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException,
			InterruptedException, SEPABindingsException {
		logger.debug("Join the chat");

		subscribe();
	
		logger.debug("Joined");
	}

	public void leaveChat()
			throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("Leave the chat");

		unsubscribe(new JSAPProvider().getTimeout(), new JSAPProvider().getNRetry());

		logger.info("Leaved");
	}
}
