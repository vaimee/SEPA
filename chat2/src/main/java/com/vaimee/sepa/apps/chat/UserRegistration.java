package com.vaimee.sepa.apps.chat;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.pattern.Producer;

public class UserRegistration extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public UserRegistration() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAPProvider().getJsap(), "REGISTER_USER");
	}
	
	public void register(String userName) {
		logger.debug("Register: "+userName);
		
		try {
			this.setUpdateBindingValue("userName", new RDFTermLiteral(userName));
			//this.setUpdateBindingValue("user", new RDFTermURI("http://wot.arces.unibo.it/chat/user/"+userName));
			
			update();
		} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
			logger.error(e.getMessage());
		}
	}

}
