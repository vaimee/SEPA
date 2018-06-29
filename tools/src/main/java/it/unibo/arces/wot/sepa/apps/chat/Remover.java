package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
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

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret;
		try {
			ret = subscribe();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
			return false;
		}
		joined = !ret.isError();

		if (joined)
			onAddedResults(((SubscribeResponse) ret).getBindingsResults());

		return joined;
	}

	public boolean leaveChat() {
		if (!joined)
			return true;

		Response ret;
		try {
			ret = unsubscribe();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
			return false;
		}
		joined = !ret.isUnsubscribeResponse();

		return !joined;
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
		
		while (!joinChat()) {
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

}
