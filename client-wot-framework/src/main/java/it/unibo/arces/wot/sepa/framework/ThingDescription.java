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
	private EventPublisher events;
	private PropertyChangeEventPublisher propertyChangeEvents;
	private TargetPropertyPublisher targetProperties;
	private ProtocolPublisher protocols;
	private ThingDescriptionPublisher thingDescription;
	
	private RDFTermURI thing;
	
	public ThingDescription(String thingURI,String name)
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		
		app = new ApplicationProfile("td.jsap");
		
		this.properties = new PropertyPublisher();
		this.actions = new ActionPublisher();
		this.events = new EventPublisher();
		this.propertyChangeEvents = new PropertyChangeEventPublisher();
		this.targetProperties = new TargetPropertyPublisher();
		this.thingDescription = new ThingDescriptionPublisher();
		
		Bindings bind = new Bindings();
		thing = new RDFTermURI(thingURI);
		bind.addBinding("thing", thing);
		bind.addBinding("thingName", new RDFTermLiteral(name));
		thingDescription.update(bind);
	}
	
	public void addProperty(String propertyUUID,String propertyUUIDName,String dataTypeURI,String propertyUUIDStability,String propertyUUIDWritability,String propertyUUIDValueType,String propertyUUIDValueTypeContent){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("propertyUUID", new RDFTermURI(propertyUUID));
		bind.addBinding("propertyUUIDName", new RDFTermLiteral(propertyUUIDName));
		bind.addBinding("propertyUUIDStability", new RDFTermURI(propertyUUIDStability));
		bind.addBinding("propertyUUIDWritability", new RDFTermLiteral(propertyUUIDWritability));
		bind.addBinding("propertyUUIDValueType", new RDFTermURI(propertyUUIDValueType));
		bind.addBinding("dataTypeURI", new RDFTermURI(dataTypeURI));
		bind.addBinding("propertyUUIDValueTypeContent", new RDFTermLiteral(propertyUUIDValueTypeContent));
		properties.update(bind);
	}
	
	public void addAction(String actionUUID,String actionUUIDName){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("actionUUID", new RDFTermURI(actionUUID));
		bind.addBinding("actionUUIDName", new RDFTermLiteral(actionUUIDName));
		actions.update(bind);
	}

	public void addEvent(String eventUUID,String eventUUIDName){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("eventUUID", new RDFTermURI(eventUUID));
		bind.addBinding("eventUUIDName", new RDFTermLiteral(eventUUIDName));
		events.update(bind);
	}
	
	public void addPropertyChangedEvent(String eventUUID,String eventUUIDName){	
		Bindings bind = new Bindings();
		bind.addBinding("thing", thing);
		bind.addBinding("eventUUID", new RDFTermURI(eventUUID));
		bind.addBinding("eventUUIDName", new RDFTermLiteral(eventUUIDName));
		propertyChangeEvents.update(bind);
	}
	
	public void addTargetProperty(String action_OR_event,String targetPropertyUUID){	
		Bindings bind = new Bindings();
		bind.addBinding("action_OR_event", new RDFTermURI(action_OR_event));
		bind.addBinding("targetPropertyUUID", new RDFTermURI(targetPropertyUUID));
		targetProperties.update(bind);
	}
	
	public void addProtocol(String actionUUID,String protocolUUID){	
		Bindings bind = new Bindings();
		bind.addBinding("actionUUID", new RDFTermURI(actionUUID));
		bind.addBinding("protocolUUID", new RDFTermURI(protocolUUID));
		protocols.update(bind);
	}
	
	class ThingDescriptionPublisher extends Producer {

		public ThingDescriptionPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "INIT_TD");
		}		
	}
	
	class PropertyPublisher extends Producer {

		public PropertyPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "ADD_PROPERTY");
		}		
	}
	
	class ActionPublisher extends Producer {

		public ActionPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "ADD_ACTION");
		}		
	}
	
	class EventPublisher extends Producer {

		public EventPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "ADD_EVENT");
		}		
	}
	
	class PropertyChangeEventPublisher extends Producer {

		public PropertyChangeEventPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "ADD_PROPERTY_CHANGED_EVENT");
		}		
	}
	
	class TargetPropertyPublisher extends Producer {

		public TargetPropertyPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "APPEND_TARGET_PROPERTY_TO_ACTION_OR_EVENT");
		}		
	}
	
	class ProtocolPublisher extends Producer {

		public ProtocolPublisher()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "APPEND_ACCESS_PROTOCOL_TO_ACTION");
		}		
	}
}
