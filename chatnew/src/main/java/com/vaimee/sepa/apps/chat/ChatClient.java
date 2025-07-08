package com.vaimee.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;


/**
 * A chat client is composed by three SEPA clients:
 * 1) Sender : it sends a message to a client
 * 2) Receiver: it receives notifications about messages that have been sent or removed. It marks messages that have been sent
 * to the client as received.
 * 3) Remover: it removes messages that have been received 
 * */
public abstract class ChatClient implements Runnable,IMessageHandler {
	protected static final Logger logger = LogManager.getLogger();
	
	protected Sender sender;
	private Receiver receiver;
	private Remover remover;
	
	public ChatClient(String userURI) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, IOException, InterruptedException {
		sender = new Sender(userURI,this);
		receiver = new Receiver(userURI,this);
		remover = new Remover(userURI,this);
		
		do {
			logger.info(userURI + " joining the chat...");
			try {
				joinChat();
			} catch (SEPASecurityException | IOException | SEPAPropertiesException | SEPAProtocolException
					| InterruptedException | SEPABindingsException e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					return;
				}
				continue;
			}
			break;
		} while (true);

		logger.info(userURI + " chat joined!");
	}
	
	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException, SEPABindingsException {
		remover.joinChat();
		receiver.joinChat();
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		remover.leaveChat();
		receiver.leaveChat();
	}

	public boolean sendMessage(String receiverURI,String message) {
		return sender.sendMessage(receiverURI,message);
	}
}
