package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class PingPongClient extends BasicClient {
	private int index = 0;
	
	public PingPongClient(String userURI, Users users)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(userURI, users,1);
	}
	
	@Override
	public void onMessage(String from,String message) {
		super.onMessage(from,message);
		sendMessage(from, "Reply #" + index++);
	}

}
