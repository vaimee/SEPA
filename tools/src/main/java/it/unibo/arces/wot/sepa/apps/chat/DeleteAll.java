package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class DeleteAll extends Producer {

	public DeleteAll() throws SEPAProtocolException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "DELETE_ALL");
	}
	
	public void clean() {
		update(null);
	}

}
