package it.unibo.arces.wot.sepa.apps.chat.client;

import java.io.IOException;

import it.unibo.arces.wot.sepa.apps.chat.Receiver;
import it.unibo.arces.wot.sepa.apps.chat.Remover;
import it.unibo.arces.wot.sepa.apps.chat.Sender;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public abstract class ChatClient implements Runnable {
	
	protected Sender sender;
	private Receiver receiver;
	private Remover remover;
	protected String userURI;
	
	public ChatClient(JSAP jsap,String userURI,SEPASecurityManager sm) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		this.userURI = userURI;
		sender = new Sender(jsap,userURI,sm);
		receiver = new Receiver(jsap,userURI,this,sm);
		remover = new Remover(jsap,userURI,this,sm);
	}
	
	public void joinChat() throws SEPASecurityException, IOException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
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
	
	public abstract void onMessage(String from,String message);
	public abstract void onMessageRemoved(String messageURI);
}
