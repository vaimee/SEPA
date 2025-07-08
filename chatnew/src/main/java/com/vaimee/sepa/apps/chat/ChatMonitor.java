package com.vaimee.sepa.apps.chat;

import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;

public class ChatMonitor extends Thread {
	protected static final Logger logger = LogManager.getLogger();

	boolean allDone;

	private HashMap<String, UserMonitor> messageMap = new HashMap<>();

	public void run() {
		while (!allDone) {
			try {
				monitor();
			} catch (InterruptedException e) {
				return;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	public ChatMonitor(Set<String> users, int messages) throws SEPAProtocolException, SEPAPropertiesException,
			SEPASecurityException, SEPABindingsException, InterruptedException {

		for (String user : users)
			messageMap.put(user, new UserMonitor(user, messages * (users.size() - 1)));
	}

	private synchronized void monitor() throws InterruptedException {
		logger.info("************************************");
		logger.info("user  messages|sent|received|removed");
		logger.info("************************************");
		
		allDone = true;
		for (UserMonitor mon : messageMap.values()) {
			logger.info(mon);
			logger.debug(mon.printDetails());
			allDone = allDone && mon.allDone();
		}
	}

	public synchronized void messageSent(String user,String to) {
		messageMap.get(user).setSent(to);
	}

	public synchronized void messageReceived(String user,String from) {
		messageMap.get(user).setReceived(from);
	}

	public synchronized void messageRemoved(String user,String to) {
		messageMap.get(user).setRemoved(to);
	}
}
