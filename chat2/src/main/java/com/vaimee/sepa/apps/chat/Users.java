package com.vaimee.sepa.apps.chat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.sparql.ARBindingsResults;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.BindingsResults;
import com.vaimee.sepa.api.pattern.Consumer;

public class Users extends Consumer {
	private static final Logger logger = LogManager.getLogger();

	private HashMap<String, String> usersList = new HashMap<String, String>();
	private boolean usersRetrieved = false;

	public Users() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new JSAPProvider().getJsap(), "USERS");
	}

	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException,
			InterruptedException, SEPABindingsException {
		logger.debug("Joining...");

		subscribe(new JSAPProvider().getTimeout(), new JSAPProvider().getNRetry());
		synchronized (this) {
			wait(1000);
		}

		logger.debug("Joined");

		logger.debug("Retrive users...");
		while (!usersRetrieved) {
			synchronized (this) {
				wait(1000);
			}
		}
		logger.debug("Users retrieved");
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException,
			InterruptedException {

		unsubscribe(new JSAPProvider().getTimeout(), new JSAPProvider().getNRetry());
		synchronized (this) {
			wait(1000);
		}
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
	public void onFirstResults(BindingsResults results) {
		synchronized (usersList) {
			for (Bindings bindings : results.getBindings()) {
				usersList.put(bindings.getValue("user"), bindings.getValue("userName"));
				logger.debug("Add user: " + bindings.getValue("userName"));
			}
		}

		synchronized (this) {
			usersRetrieved = true;
			notify();
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {
		synchronized (usersList) {
			for (Bindings bindings : results.getRemovedBindings().getBindings()) {
				usersList.remove(bindings.getValue("user"));
				logger.debug("Remove user: " + bindings.getValue("user"));
			}
			for (Bindings bindings : results.getAddedBindings().getBindings()) {
				usersList.put(bindings.getValue("user"), bindings.getValue("userName"));
				logger.debug("Add user: " + bindings.getValue("userName"));
			}
		}
	}
}
