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

public class Receiver extends Aggregator {
	private static final Logger logger = LogManager.getLogger();
	
	private boolean joined = false;
	
	private ChatClient client;
	
	public Receiver(String receiverURI,ChatClient client)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAP("chat.jsap"), "SENT", "SET_RECEIVED");

		this.setSubscribeBindingValue("receiver", new RDFTermURI(receiverURI));
		
		this.client = client;
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
	public void onAddedResults(BindingsResults results) {
		// Variables: ?message ?sender ?name ?text ?time
		for (Bindings bindings : results.getBindings()) {
			logger.info("SENT "+bindings.getValue("message"));
			client.onMessage(bindings.getValue("sender"), bindings.getValue("text"));
			
			// Set received
			this.setUpdateBindingValue("message", new RDFTermURI(bindings.getValue("message")));
			try {
				update();
			} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}			
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// Variables: ?message ?sender ?name ?text ?time
		for (Bindings bindings : results.getBindings()) {
			logger.info("REMOVED "+bindings.getValue("message"));
			
			client.onMessageRemoved(bindings.getValue("message"));
		}
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
