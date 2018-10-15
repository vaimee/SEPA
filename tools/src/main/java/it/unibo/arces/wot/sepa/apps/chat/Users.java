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
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Users extends Consumer {
	private static final Logger logger = LogManager.getLogger();
	
	private HashMap<String, String> usersList = new HashMap<String, String>();
	private boolean joined = false;
	private boolean firstResults = false;

	public Users(JSAP jsap,SEPASecurityManager sm) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(jsap, "USERS",sm);
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		logger.debug("joinChat");
		while (!joined) {
			subscribe(5000);
			synchronized(this) {
				wait(5000);
			}
		}
		while (!firstResults) {
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

	public Set<String> getUsers() {
		logger.debug("getUsers");
		synchronized (usersList) {
			return usersList.keySet();
		}
	}

	public String getUserName(String user) {
		logger.debug("getUserName: "+user);
		synchronized (usersList) {
			return usersList.get(user);
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
		logger.debug("onResults");
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		logger.debug("onAddedResults");
		synchronized (usersList) {
			for (Bindings bindings : results.getBindings()) {
				usersList.put(bindings.getValue("user"), bindings.getValue("userName"));
			}
		}
		synchronized(this) {
			firstResults = true;
			notify();
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		logger.debug("onRemovedResults");
		synchronized (usersList) {
			for (Bindings bindings : results.getBindings()) {
				usersList.remove(bindings.getValue("user"));
			}
		}
	}

	@Override
	public void onBrokenConnection() {
		logger.warn("onBrokenConnection");
		joined = false;
		
		try {
			joinChat();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPAProtocolException
				| InterruptedException e2) {
		}
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error("onError:" +errorResponse);
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
