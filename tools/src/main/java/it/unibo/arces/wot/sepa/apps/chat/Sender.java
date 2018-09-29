package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;
import it.unibo.arces.wot.sepa.timing.Timings;

public class Sender extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	private String sender;
	
	public Sender(String senderURI)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAP("chat.jsap"), "SEND");
		
		this.setUpdateBindingValue("sender", new RDFTermURI(senderURI));
		this.sender = senderURI;
	}
	
	public boolean sendMessage(String receiverURI,String text) {
		logger.info("SEND From: "+sender+" To: "+receiverURI+" "+text);
		
		this.setUpdateBindingValue("receiver", new RDFTermURI(receiverURI));
		this.setUpdateBindingValue("text", new RDFTermLiteral(text));
		
		long start = Timings.getTime();
		boolean ret = false;
		try {
			ret = update().isUpdateResponse();
		} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		long stop = Timings.getTime();
		String msg = sender+receiverURI+text;
		Timings.log(msg.replace(" ", "_"), start, stop);
		return ret;
	}
}
