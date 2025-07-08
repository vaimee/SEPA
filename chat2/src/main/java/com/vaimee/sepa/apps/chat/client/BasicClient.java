package com.vaimee.sepa.apps.chat.client;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.apps.chat.ChatClient;
import com.vaimee.sepa.apps.chat.ChatMonitor;
import com.vaimee.sepa.apps.chat.Users;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;

public class BasicClient extends ChatClient {
	private static final Logger logger = LogManager.getLogger();

	protected final String user;
	private final Users users;
	private final int messages;
	private final ChatMonitor monitor;
	
	public BasicClient(String userURI, Users users,int messages,ChatMonitor monitor)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, IOException, InterruptedException {
		super(userURI);

		this.user = userURI;
		this.users = users;
		this.messages = messages;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		int n = 0;
		
		for (int i = 0; i < messages; i++) {
			for (String receiver : users.getUsers()) {
				if (receiver.equals(user) && users.getUsers().size() > 1)
					continue;
				n++;
				logger.debug(users.getUserName(user) + " SEND MESSAGE (" + n + "/" + messages *  (users.getUsers().size()-1) +")");
				sendMessage(receiver, "MSG #" + n);
			}
		}

		try {
			synchronized (this) {
				wait();
			}
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void onMessageReceived(String userUri, String toUri,String messageUri, String name, String message,String time) {
		monitor.messageReceived(userUri,toUri);
	}

	@Override
	public void onMessageRemoved(String userUri, String toUri, String messageUri, String name, String message, String time) {
		monitor.messageRemoved(userUri,toUri);
	}

	@Override
	public void onMessageSent(String userUri, String toUri,String messageUri, String time) {
		monitor.messageSent(userUri,toUri);
	}
}
