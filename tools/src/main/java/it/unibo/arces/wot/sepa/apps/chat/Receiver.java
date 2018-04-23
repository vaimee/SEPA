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

public class Receiver extends Aggregator {
	private static final Logger logger = LogManager.getLogger();
	
	private Bindings message = new Bindings();
	private boolean joined = false;
	private Timings timings;
	
	private ChatClient client;
	
	public Receiver(String receiverURI,Timings timings,ChatClient client)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "SENT", "SET_RECEIVED");

		message.addBinding("receiver", new RDFTermURI(receiverURI));
		
		this.timings = timings;
		this.client = client;
	}

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret = subscribe(message);
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
	public void onAddedResults(BindingsResults results) {
		// Variables: ?message ?sender ?name ?text ?time
		for (Bindings bindings : results.getBindings()) {
			logger.info("SENT "+bindings.getBindingValue("message"));
			
			timings.sent(bindings.getBindingValue("message"),bindings.getBindingValue("sender"),message.getBindingValue("receiver"),bindings.getBindingValue("text"));
			
			client.onMessage(bindings.getBindingValue("sender"), bindings.getBindingValue("text"));
			
			// Set received
			Bindings setReceived = new Bindings();
			setReceived.addBinding("message", new RDFTermURI(bindings.getBindingValue("message")));

			timings.setReceivedStart(bindings.getBindingValue("message"));
			update(setReceived);
			timings.setReceivedStop(bindings.getBindingValue("message"));
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// Variables: ?message ?sender ?name ?text ?time
		for (Bindings bindings : results.getBindings()) {
			logger.info("REMOVED "+bindings.getBindingValue("message"));
			
			client.onMessageRemoved(bindings.getBindingValue("message"));
		}
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
