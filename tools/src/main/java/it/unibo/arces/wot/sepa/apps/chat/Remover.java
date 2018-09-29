package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Remover extends Aggregator {
	private static final Logger logger = LogManager.getLogger();

	private boolean joined = false;
	
	public Remover(String senderURI,ChatClient client) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAP("chat.jsap"), "RECEIVED", "REMOVE");
		
		this.setSubscribeBindingValue("sender", new RDFTermURI(senderURI));
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		if (!joined) subscribe(5000);
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		if (joined) unsubscribe(5000);
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			logger.info("RECEIVED From: "+bindings.getValue("message"));
			
			// Variables: ?message 
			this.setUpdateBindingValue("message", bindings.getRDFTerm("message"));
			try {
				update();
			} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		}

	}

	@Override
	public void onRemovedResults(BindingsResults results) {

	}

	@Override
	public void onBrokenConnection() {
		joined = false;
		
		while (!joined) {
			try {
				joinChat();
			} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPAProtocolException e1) {
				
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		joined = true;
	}

	@Override
	public void onUnsubscribe(String spuid) {
		joined = false;
	}
}
