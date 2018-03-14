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

public class Remover extends Aggregator {
	private Bindings sender = new Bindings();
	private boolean joined = false;
	
	public Remover(String senderURI) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new ApplicationProfile("chat.jsap"), "RECEIVED", "REMOVE");
		sender.addBinding("sender", new RDFTermURI(senderURI));
	}

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret = subscribe(sender);
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
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			System.out.println("Remove "+bindings);
			update(bindings);
		}
		
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPing() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBrokenSocket() {
		joined = false;
		
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub
		
	}

}
