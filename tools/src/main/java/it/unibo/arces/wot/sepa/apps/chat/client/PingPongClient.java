package it.unibo.arces.wot.sepa.apps.chat.client;

import it.unibo.arces.wot.sepa.apps.chat.Users;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class PingPongClient extends BasicClient {
	private int index = 0;
	
	public PingPongClient(JSAP jsap,String userURI, Users users,SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(jsap, userURI, users,1,sm);
	}
	
	@Override
	public void onMessage(String from,String message) {
		super.onMessage(from,message);
		sendMessage(from, "Reply #" + index++);
	}

}
