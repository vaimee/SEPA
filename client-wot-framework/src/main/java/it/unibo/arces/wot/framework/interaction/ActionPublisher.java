package it.unibo.arces.wot.framework.interaction;

import java.util.UUID;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class ActionPublisher extends Producer {
	private ApplicationProfile app;

	private String action;
	private ActionPubliserWithInput publisherWithInput;

	class ActionPubliserWithInput extends Producer {

		public ActionPubliserWithInput() throws SEPAProtocolException, SEPASecurityException {
			super(app, "POST_ACTION_WITH_INPUT");
		}

	}

	public ActionPublisher(String action) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("td.jsap"), "POST_ACTION");

		this.app = new ApplicationProfile("td.jsap");
		this.action = action;
		this.publisherWithInput = new ActionPubliserWithInput();
	}

	public ActionPublisher(ApplicationProfile app, String action) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("td.jsap"), "POST_ACTION");

		this.app = app;
		this.action = action;
		this.publisherWithInput = new ActionPubliserWithInput();
	}

	public void post() {
		Bindings bind = new Bindings();
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("newInstance", new RDFTermURI("wot:" + UUID.randomUUID()));
		update(bind);
	}

	public void post(String value, String dataTypeURI) {
		Bindings bind = new Bindings();
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("value", new RDFTermLiteral(value));
		publisherWithInput.update(bind);
	}
}
