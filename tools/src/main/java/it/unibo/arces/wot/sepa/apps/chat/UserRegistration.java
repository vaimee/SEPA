package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class UserRegistration extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public UserRegistration(JSAP jsap,SEPASecurityManager sm) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(jsap, "REGISTER_USER",sm);
	}
	
	public void register(String userName) {
		logger.debug("Register: "+userName);
		this.setUpdateBindingValue("userName", new RDFTermLiteral(userName));
		try {
			update();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
	}

}
