package it.unibo.arces.wot.framework;

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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class ThingDescription { 
	private ApplicationProfile app;
	
	private PropertyPublisher properties;
	private ActionPublisher actions;
	private ActionWithInputPublisher actionsWithInput;
	private EventPublisher events;
	private EventWithOutputPublisher eventsWithOutput;
	private PropertyChangeEventPublisher propertyChangeEvents;
	private TargetPropertyPublisher targetProperties;
	private ProtocolPublisher protocols;
	private ThingDescriptionPublisher thingDescription;
	
	private RDFTermURI thing;
	
	public ThingDescription(ApplicationProfile app,String thingURI,String name)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		
		this.app = app;		
		this.properties = new PropertyPublisher();
		this.actions = new ActionPublisher();
		this.actionsWithInput = new ActionWithInputPublisher();
		this.events = new EventPublisher();
		this.eventsWithOutput = new EventWithOutputPublisher();
		this.propertyChangeEvents = new PropertyChangeEventPublisher();
		this.targetProperties = new TargetPropertyPublisher();
		this.thingDescription = new ThingDescriptionPublisher();
		
		Bindings bind = new Bindings();
		thing = new RDFTermURI(thingURI);
		bind.addBinding("thing", thing);
		bind.addBinding("name", new RDFTermLiteral(name));
		thingDescription.update(bind);
	}
	
	public ThingDescription(String thingURI,String name)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		this(new ApplicationProfile("td.jsap"),thingURI,name);
	}
	
	public void addProperty(String property,String name,String dataType,String stability,String writable,String value){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("property", new RDFTermURI(property));
		bind.addBinding("name", new RDFTermLiteral(name));
		bind.addBinding("stability", new RDFTermLiteral(stability));
		bind.addBinding("writable", new RDFTermLiteral(writable));
		bind.addBinding("dataType", new RDFTermURI(dataType));
		bind.addBinding("value", new RDFTermLiteral(value));
		properties.update(bind);
	}
	
	public void addAction(String action,String name,String protocol){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("name", new RDFTermLiteral(name));
		bind.addBinding("protocol", new RDFTermURI(protocol));
		actions.update(bind);
	}
	
	public void addAction(String action,String name,String protocol,String dataType){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("name", new RDFTermLiteral(name));
		bind.addBinding("protocol", new RDFTermURI(protocol));
		bind.addBinding("dataType", new RDFTermURI(dataType));
		actionsWithInput.update(bind);
	}

	public void addEvent(String event,String name){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("event", new RDFTermURI(event));
		bind.addBinding("name", new RDFTermLiteral(name));
		events.update(bind);
	}
	
	public void addEvent(String event,String name,String dataType){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("event", new RDFTermURI(event));
		bind.addBinding("name", new RDFTermLiteral(name));
		bind.addBinding("dataType", new RDFTermURI(dataType));
		eventsWithOutput.update(bind);
	}
	
	public void addPropertyChangedEvent(String event,String name,String dataType){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("event", new RDFTermURI(event));
		bind.addBinding("name", new RDFTermLiteral(name));
		bind.addBinding("dataType", new RDFTermURI(dataType));
		propertyChangeEvents.update(bind);
	}
	
	public void addTargetProperty(String action_OR_event,String property){	
		Bindings bind = new Bindings();
		bind.addBinding("action_OR_event", new RDFTermURI(action_OR_event));
		bind.addBinding("property", new RDFTermURI(property));
		targetProperties.update(bind);
	}
	
	public void addProtocol(String action,String protocol){	
		Bindings bind = new Bindings();
		bind.addBinding("action", new RDFTermURI(action));
		bind.addBinding("protocol", new RDFTermURI(protocol));
		protocols.update(bind);
	}
	
	class ThingDescriptionPublisher extends Producer {

		public ThingDescriptionPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_INIT");
		}		
	}
	
	class PropertyPublisher extends Producer {

		public PropertyPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_PROPERTY");
		}		
	}
	
	class ActionPublisher extends Producer {

		public ActionPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_ACTION");
		}		
	}
	
	class ActionWithInputPublisher extends Producer {

		public ActionWithInputPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_ACTION_WITH_INPUT");
		}		
	}
	
	class EventPublisher extends Producer {

		public EventPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_EVENT");
		}		
	}
	
	class EventWithOutputPublisher extends Producer {

		public EventWithOutputPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_EVENT_WITH_OUTPUT");
		}		
	}
	
	class PropertyChangeEventPublisher extends Producer {

		public PropertyChangeEventPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_ADD_PROPERTY_CHANGED_EVENT");
		}		
	}
	
	class TargetPropertyPublisher extends Producer {

		public TargetPropertyPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_APPEND_TARGET_PROPERTY_TO_ACTION_OR_EVENT");
		}		
	}
	
	class ProtocolPublisher extends Producer {

		public ProtocolPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "TD_APPEND_ACCESS_PROTOCOL_TO_ACTION");
		}		
	}
}
