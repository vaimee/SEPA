package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public abstract class ChatClient implements Runnable {
	
	protected Sender sender;
	private Receiver receiver;
	private Remover remover;
	protected String userURI;
	
	public ChatClient(String userURI) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		this.userURI = userURI;
		sender = new Sender(userURI);
		receiver = new Receiver(userURI,this);
		remover = new Remover(userURI,this);
	}
	
	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		remover.joinChat();
		receiver.joinChat();
	}

	public void leaveChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException {
		remover.leaveChat();
		receiver.leaveChat();
	}

	public boolean sendMessage(String receiverURI,String message) {
		return sender.sendMessage(receiverURI,message);
	}
	
	public abstract void onMessage(String from,String message);
	public abstract void onMessageRemoved(String messageURI);
}
