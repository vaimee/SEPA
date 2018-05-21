package it.unibo.arces.wot.sepa.apps.chat;

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
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class Remover extends Aggregator {
	private static final Logger logger = LogManager.getLogger();
	
	private Bindings sender = new Bindings();
	private boolean joined = false;
	
	public Remover(String senderURI,ChatClient client) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new ApplicationProfile("chat.jsap"), "RECEIVED", "REMOVE");
		
		sender.addBinding("sender", new RDFTermURI(senderURI));
	}

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret = subscribe(sender);
		joined = !ret.isError();

		if (joined)
			onAddedResults(((SubscribeResponse) ret).getBindingsResults());

		return joined;
	}

	public boolean leaveChat() {
		if (!joined)
			return true;

		Response ret = unsubscribe();
		joined = !ret.isUnsubscribeResponse();

		return !joined;
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			logger.info("RECEIVED From: "+bindings.getBindingValue("message"));
			
			// Variables: ?message ?time
			update(bindings);
		}

	}

	@Override
	public void onRemovedResults(BindingsResults results) {

	}

	@Override
	public void onBrokenSocket() {
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
