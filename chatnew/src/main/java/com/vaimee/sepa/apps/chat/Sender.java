package com.vaimee.sepa.apps.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.commons.sparql.RDFTermURI;
import com.vaimee.sepa.api.pattern.Producer;

class Sender extends Producer {
	protected static final Logger logger = LogManager.getLogger();

	private final String userUri;
	
	public Sender(String userUri,IMessageHandler handler)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(new JSAPProvider().getJsap(), "SEND");

		this.setUpdateBindingValue("sender", new RDFTermURI(userUri));
		
		this.userUri = userUri;
	}

	public boolean sendMessage(String receiverURI, String text) {
		logger.debug("SEND To: " + receiverURI + " Message: " + text);

		int retry = 5;

		boolean ret = false;
		while (!ret && retry > 0) {
			try {
				this.setUpdateBindingValue("receiver", new RDFTermURI(receiverURI));
				this.setUpdateBindingValue("text", new RDFTermLiteral(text));

				ret = update().isUpdateResponse();
			} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException
					| SEPABindingsException e) {
				logger.error(e.getMessage());
				ret = false;
			}
			retry--;
		}
		
		if (!ret) logger.error("UPDATE FAILED sender: "+userUri+" receiver: "+receiverURI+" text: "+text);

		return ret;
	}
}
