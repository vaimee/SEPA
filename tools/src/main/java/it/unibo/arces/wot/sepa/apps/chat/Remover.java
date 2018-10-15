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

public class Remover extends Aggregator {
	private static final Logger logger = LogManager.getLogger();

	private boolean joined = false;
	
	public Remover(JSAP jsap,String senderURI,ChatClient client,SEPASecurityManager sm) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(jsap, "RECEIVED", "REMOVE",sm);
		
		this.setSubscribeBindingValue("sender", new RDFTermURI(senderURI));
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("joinChat");
		while (!joined) {
			subscribe(5000);
			synchronized(this) {
				wait(5000);
			}
		}
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("leaveChat");
		while (joined) {
			unsubscribe(5000);
			synchronized(this) {
				wait(5000);
			}
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
		logger.debug("onResults");
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		logger.debug("onAddedResults");
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
		logger.debug("onRemovedResults");
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
