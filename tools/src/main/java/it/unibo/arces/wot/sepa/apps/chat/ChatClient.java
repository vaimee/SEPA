package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public abstract class BasicChatClient implements ChatListener {

	private MessageSender sender;
	private MessageReceiver receiver;
	private String nickname;
	
	public BasicChatClient(String nickname,ApplicationProfile app) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		sender = new MessageSender(nickname,app,this);
		receiver = new MessageReceiver(nickname,app,this);
		this.nickname = nickname;
	}
	
	public String getNickname() {
		return nickname;
	}

	public boolean joinChat() {
		if (!sender.joinChat()) return false;
		if (!receiver.joinChat()) return false;
		return true;
	}

	public boolean leaveChat() {
		if (!sender.leaveChat()) return false;
		if (!receiver.leaveChat()) return false;
		return true;
	}

	public boolean sendMessage(String receiver,String message) {
		return sender.sendMessage(receiver,message);
	}
}
