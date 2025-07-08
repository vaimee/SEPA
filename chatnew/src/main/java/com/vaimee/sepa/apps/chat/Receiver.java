package com.vaimee.sepa.apps.chat;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.commons.sparql.RDFTermURI;

class Receiver extends ChatAggregator {
	private final IMessageHandler handler;
	private String userUri;
	
	public Receiver(String userUri,IMessageHandler handler)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super("SENT", "SET_RECEIVED");

		this.setSubscribeBindingValue("receiver", new RDFTermURI(userUri));
		
		this.handler = handler;
		this.userUri = userUri;
	}
	
	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			logger.debug("SENT " + bindings.getValue("message") +" From: "+bindings.getValue("sender")+ " To: "+userUri);
			
			handler.onMessageReceived(userUri, bindings.getValue("sender"),bindings.getValue("message"), bindings.getValue("name"), bindings.getValue("text"),bindings.getValue("time"));
			
			try {
				this.setUpdateBindingValue("receiver", new RDFTermURI(userUri));
				this.setUpdateBindingValue("sender", new RDFTermURI(bindings.getValue("sender")));
				this.setUpdateBindingValue("sentTime", new RDFTermLiteral(bindings.getValue("time")));
			} catch (SEPABindingsException e) {
				logger.error(e.getMessage());
			}
			
			try {
				update();	
			} catch (SEPASecurityException | SEPAProtocolException | SEPAPropertiesException | SEPABindingsException e) {
				logger.error(e.getMessage());
			}
		}
	}
	
	@Override
	public void onRemovedResults(BindingsResults results) {		
		for (Bindings bindings : results.getBindings()) {
			logger.debug("REMOVED " + bindings.getValue("message"));
			
			handler.onMessageRemoved(userUri, bindings.getValue("sender"),bindings.getValue("message"), bindings.getValue("name"), bindings.getValue("text"),bindings.getValue("time"));
		}
	}
}
