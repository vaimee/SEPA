package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class PingPongClient extends ChatClient implements Runnable {
	private int index = 0;
	private String user;
	private Users users;
	private ChatListener listener;
	
	public PingPongClient(String userURI,Users users,ChatListener listener) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(userURI);
		this.user = userURI;
		this.users = users;
		this.listener = listener;
	}

	@Override
	public void onMessageReceived(Message message) {
		listener.onMessageReceived(message);
		sendMessage(message.getFrom(), "Reply #" + index++);
	}

	@Override
	public void onBrokenConnection() {
		System.out.println(user + " connection is down!");

		while (!joinChat()) {
			try {
				System.out.println("Joining the chat...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		System.out.println("Chat joined!");
	}
	
	@Override
	public boolean sendMessage(String receiverURI,String message) {
		if (receiverURI.equals(user)) return false;
		return sender.sendMessage(receiverURI,message);
	}

	@Override
	public void run() {
		while (!joinChat()) {
			try {
				System.out.println("Joining the chat...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		System.out.println("Chat joined!");
		
		for (String receiver : users.getUsers()) {
			if (receiver.equals(user)) continue;
			sendMessage(receiver,"Hello!");
		}
		
		try {
			synchronized(this) {wait();}
		} catch (InterruptedException e) {
		}
	}

}
