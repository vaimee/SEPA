package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class DeleteAll extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public DeleteAll() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAP("chat.jsap"), "DELETE_ALL");
	}
	
	public void clean() {
		try {
			update();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
	}

}
