package it.unibo.arces.wot.sepa.apps.chat;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class UserRegistration extends Producer {

	public UserRegistration() throws SEPAProtocolException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), "REGISTER_USER");
	}
	
	public void register(String userName) {
		Bindings bindings = new Bindings();
		bindings.addBinding("userName", new RDFTermLiteral(userName));
		update(bindings);
	}

}
