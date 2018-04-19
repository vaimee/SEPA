package it.unibo.arces.wot.sepa.apps.chat;

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
	private Bindings message = new Bindings();

	private ChatListener listener;
	private boolean joined = false;
	
	public Receiver(String receiverURI, ChatListener listener)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "SENT", "SET_RECEIVED");

		message.addBinding("receiver", new RDFTermURI(receiverURI));

		this.listener = listener;
	}

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret = subscribe(message);
		joined = !ret.isError();
				
		if (joined) onAddedResults(((SubscribeResponse) ret).getBindingsResults());

		return joined;
	}

	public boolean leaveChat() {
		if(!joined) return true;
		
		Response ret = unsubscribe();	
		joined = !ret.isUnsubscribeResponse();
		
		return !joined;
	}

	@Override
	public void onAddedResults(BindingsResults results) {

		for (Bindings bindings : results.getBindings()) {
			// Set received
			Bindings setReceived = new Bindings();
			setReceived.addBinding("message", new RDFTermURI(bindings.getBindingValue("message")));
			update(setReceived);
						
			listener.onMessageReceived(
					new Message(
							bindings.getBindingValue("sender"), 
							message.getBindingValue("receiver"),
							bindings.getBindingValue("text"), 
							bindings.getBindingValue("time")));
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {}

	@Override
	public void onRemovedResults(BindingsResults results) {}

	@Override
	public void onPing() {}

	@Override
	public void onBrokenSocket() {
		joined = false;
	}

	@Override
	public void onError(ErrorResponse errorResponse) {}
}
