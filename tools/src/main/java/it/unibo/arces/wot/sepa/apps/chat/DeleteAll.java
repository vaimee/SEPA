package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class DeleteAll extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public DeleteAll(JSAP jsap, SEPASecurityManager sm) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(jsap, "DELETE_ALL",sm);
	}
	
	public void clean() {
		logger.info("Delete all");
		try {
			update();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(e.getMessage());
		}
	}

}
