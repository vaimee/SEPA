package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class Sender extends Producer {

	private Bindings message = new Bindings();
	
	public Sender(String senderURI)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "SEND");
		
		message.addBinding("sender", new RDFTermURI(senderURI));
	}
	
	public boolean sendMessage(String receiverURI,String text) {
		message.addBinding("receiver", new RDFTermURI(receiverURI));
		message.addBinding("text", new RDFTermLiteral(text));
		
		return update(message).isUpdateResponse();
	}
}
