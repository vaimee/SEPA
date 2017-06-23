package it.unibo.arces.wot.sepa.framework;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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

		public ActionPubliserWithInput()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "POST_ACTION_REQUEST_WITH_INPUT");
		}
		
	}
	
	public ActionPublisher(String action)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(new ApplicationProfile("td.jsap"), "POST_ACTION_REQUEST_WITHOUT_INPUT");
		
		this.app = new ApplicationProfile("td.jsap");
		this.action = action;
		this.publisherWithInput = new ActionPubliserWithInput();
	}
	
	public void post() {
		Bindings bind = new Bindings();
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("newInstance", new RDFTermURI("wot:"+UUID.randomUUID()));
		update(bind);
	}

	public void post(String value) {
		Bindings bind = new Bindings();
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("newInstance", new RDFTermURI("wot:"+UUID.randomUUID()));
		bind.addBinding("newInput", new RDFTermURI("wot:"+UUID.randomUUID()));
		bind.addBinding("newValue", new RDFTermLiteral(value));
		publisherWithInput.update(bind);
	}
}
