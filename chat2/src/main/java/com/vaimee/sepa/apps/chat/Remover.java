package com.vaimee.sepa.apps.chat;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.commons.sparql.RDFTermURI;

class Remover extends ChatAggregator {	
	private String userUri;
	private final IMessageHandler handler;
	
	public Remover(String userUri,IMessageHandler handler) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException, SEPABindingsException {
		super("RECEIVED", "REMOVE");
		
		this.setSubscribeBindingValue("sender", new RDFTermURI(userUri));
		this.userUri = userUri;
		this.handler = handler;
	}

	@Override
	public void onAddedResults(BindingsResults results) {		
		for (Bindings bindings : results.getBindings()) {
			logger.debug("RECEIVED: "+bindings.getValue("message"));
			
			handler.onMessageSent(userUri,bindings.getValue("receiver"),bindings.getValue("message"),bindings.getValue("time"));
					
			try {
				this.setUpdateBindingValue("sender", new RDFTermURI(userUri));
				this.setUpdateBindingValue("time", new RDFTermLiteral(bindings.getValue("time")));
				update();
				
			} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
				logger.error(e.getMessage());
			}
		}
	}
}
