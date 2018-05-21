package it.unibo.arces.wot.sepa.apps.chat;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;
import it.unibo.arces.wot.sepa.timing.Timings;

public class Sender extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	private Bindings message = new Bindings();
	
	public Sender(String senderURI)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "SEND");
		
		message.addBinding("sender", new RDFTermURI(senderURI));
	}
	
	public boolean sendMessage(String receiverURI,String text) {
		logger.info("SEND From: "+message.getBindingValue("sender")+" To: "+receiverURI+" "+message);
		
		message.addBinding("receiver", new RDFTermURI(receiverURI));
		message.addBinding("text", new RDFTermLiteral(text));
		
		long start = Timings.getTime();
		boolean ret = update(message).isUpdateResponse();
		long stop = Timings.getTime();
		String msg = message.getBindingValue("sender")+message.getBindingValue("receiver")+message.getBindingValue("text");
		Timings.log(msg.replace(" ", "_"), start, stop);
		return ret;
	}
}
