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

public class EventPublisher extends Producer {
	private ApplicationProfile app;
	
	private String event;
	private EventPubliserWithOutput publisherWithOutput;
	
	class EventPubliserWithOutput extends Producer {

		public EventPubliserWithOutput()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "POST_NEW_EVENT_WITH_OUTPUT");
		}
		
	}
	
	public EventPublisher(String event)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(new ApplicationProfile("td.jsap"), "POST_NEW_EVENT_WITHOUT_OUTPUT");
		
		this.app = new ApplicationProfile("td.jsap");
		this.event = event;
		this.publisherWithOutput = new EventPubliserWithOutput();
	}
	
	public void post() {
		Bindings bind = new Bindings();
		bind.addBinding("event", new RDFTermURI(event));
		bind.addBinding("newInstance", new RDFTermURI("wot:"+UUID.randomUUID()));
		update(bind);
	}

	public void post(String value) {
		Bindings bind = new Bindings();
		bind.addBinding("event", new RDFTermURI(event));
		bind.addBinding("newInstance", new RDFTermURI("wot:"+UUID.randomUUID()));
		bind.addBinding("eNewOutput", new RDFTermURI("wot:"+UUID.randomUUID()));
		bind.addBinding("newValue", new RDFTermLiteral(value));
		publisherWithOutput.update(bind);
	}

}
