package it.unibo.arces.wot.sepa.apps.chat;

import java.util.HashMap;
import java.util.Set;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;

public class Users extends Consumer {
	private HashMap<String,String> usersList = new HashMap<String,String>();
	private boolean joined = false;
	
	public Users() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new ApplicationProfile("chat.jsap"), "USERS");
	}

	public boolean joinChat() {
		if (joined)
			return true;

		Response ret = subscribe(null);
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
	
	public Set<String> getUsers() {
		return usersList.keySet();
	}
	
	public String getUserName(String user) {
		return usersList.get(user);
	}
	
	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			usersList.put(bindings.getBindingValue("user"), bindings.getBindingValue("userName"));
		}
		
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			usersList.remove(bindings.getBindingValue("user"));
		}
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
