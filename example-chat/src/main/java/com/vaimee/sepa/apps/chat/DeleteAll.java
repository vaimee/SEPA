package com.vaimee.sepa.apps.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.pattern.Producer;

/**
 * Delete all the registered users and messages. Message logs are not delete as they belong to a different graph.
 * */
public class DeleteAll extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public DeleteAll() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAPProvider().getJsap(), "DELETE_ALL");
	}
	
	public void clean() {
		logger.info("Delete all");
		try {
			update();
		} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException | SEPAProtocolException e) {
			logger.error(e.getMessage());
		}
	}
}
