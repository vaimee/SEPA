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

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		if (!joined) subscribe(5000);
	}

	public void leaveChat() throws SEPAProtocolException, SEPASecurityException, IOException, SEPAPropertiesException {
		if (joined) unsubscribe(5000);
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
