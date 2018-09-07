package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Users extends Consumer {
	private static final Logger logger = LogManager.getLogger();
	
	private HashMap<String, String> usersList = new HashMap<String, String>();
	private boolean joined = false;

	public Users() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAP("chat.jsap"), "USERS");
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		if (!joined) subscribe(5000);
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		if (joined) unsubscribe(5000);
	}

	public Set<String> getUsers() {
		synchronized (usersList) {
			return usersList.keySet();
		}
	}

	public String getUserName(String user) {
		synchronized (usersList) {
			return usersList.get(user);
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		synchronized (usersList) {
			for (Bindings bindings : results.getBindings()) {
				usersList.put(bindings.getValue("user"), bindings.getValue("userName"));
			}
		}

	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		synchronized (usersList) {
			for (Bindings bindings : results.getBindings()) {
				usersList.remove(bindings.getValue("user"));
			}
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
		logger.error(errorResponse);
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
