package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.apps.chat.client.ChatClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
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
	
	public Receiver(JSAP jsap,String receiverURI,ChatClient client,SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(jsap, "SENT", "SET_RECEIVED",sm);

		this.setSubscribeBindingValue("receiver", new RDFTermURI(receiverURI));
		
		this.client = client;
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("Join the chat");
		while (!joined) {
			subscribe(5000);
			synchronized(this) {
				wait(5000);
			}
		}
		logger.info("Joined");
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("Leave the chat");
		while (joined) {
			unsubscribe(5000);
			synchronized(this) {
				wait(5000);
			}
		}
		logger.info("Leaved");
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		logger.debug("onAddedResults");
		
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
		logger.debug("onResults");
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		logger.debug("onRemovedResults");
		// Variables: ?message ?sender ?name ?text ?time
		for (Bindings bindings : results.getBindings()) {
			logger.info("REMOVED "+bindings.getValue("message"));
			
			client.onMessageRemoved(bindings.getValue("message"));
		}
	}

	@Override
	public void onBrokenConnection() {
		logger.warn("onBrokenConnection");
		joined = false;
		
		try {
			joinChat();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPAProtocolException | InterruptedException e2) {
		}
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("onError: "+errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("onSubscribe");
		joined = true;
		synchronized(this) {
			notify();
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("onUnsubscribe");
		joined = false;
		synchronized(this) {
			notify();
		}
	}
}
