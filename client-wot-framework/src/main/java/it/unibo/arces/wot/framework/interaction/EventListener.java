package it.unibo.arces.wot.framework.interaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import it.unibo.arces.wot.framework.elements.Event;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;

public abstract class EventListener {
	private ApplicationProfile app;
	private HashMap<String,HashMap<String,ThingEventListener>> thingEventListener = new HashMap<String,HashMap<String,ThingEventListener>>();
	private HashMap<String,AllEventListener> allEventListener = new HashMap<String,AllEventListener>();
	
	public abstract void onEvent(Set<Event> events);
	public abstract void onConnectionStatus(Boolean on);
	public abstract void onConnectionError(ErrorResponse error);
	
	public void startListeningForEvent(String eventURI) throws SEPAProtocolException, SEPASecurityException  {
		if (allEventListener.containsKey(eventURI)) return;
		AllEventListener listener = new AllEventListener(eventURI);
		allEventListener.put(eventURI, listener);
		Bindings bindings = new Bindings();
		bindings.addBinding("event", new RDFTermURI(eventURI));
		allEventListener.get(eventURI).subscribe(bindings);
	}
	
	public void stopListeningForEvent(String eventURI) {
		if (!allEventListener.containsKey(eventURI)) return;
		allEventListener.get(eventURI).unsubscribe();
		allEventListener.remove(eventURI);
	}
	
	public void startListeningForEvent(String eventURI,String thingURI) throws SEPAProtocolException, SEPASecurityException  {
		if (thingEventListener.containsKey(thingURI)) {
			HashMap<String,ThingEventListener> thingEvents = thingEventListener.get(thingURI);
			if (thingEvents.containsKey(eventURI)) return;
			ThingEventListener listener = new ThingEventListener(thingURI,eventURI);
			thingEvents.put(eventURI, listener);
			Bindings bindings = new Bindings();
			bindings.addBinding("event", new RDFTermURI(eventURI));
			bindings.addBinding("thing", new RDFTermURI(thingURI));
			thingEvents.get(eventURI).subscribe(bindings);	
		}
		else {
			HashMap<String,ThingEventListener> thingEvents = new HashMap<String,ThingEventListener>();
			ThingEventListener listener = new ThingEventListener(thingURI,eventURI);
			thingEvents.put(eventURI, listener);
			Bindings bindings = new Bindings();
			bindings.addBinding("event", new RDFTermURI(eventURI));
			bindings.addBinding("thing", new RDFTermURI(thingURI));
			thingEventListener.put(thingURI, thingEvents);
			thingEvents.get(eventURI).subscribe(bindings);	
		}
	}
	
	public void stopListeningForEvent(String eventURI,String thingURI)  {
		if (!thingEventListener.containsKey(thingURI)) return;
		if (!thingEventListener.get(thingURI).containsKey(eventURI)) return;
		thingEventListener.get(thingURI).get(eventURI).unsubscribe();
		thingEventListener.get(thingURI).remove(eventURI);
	}
	
	class AllEventListener extends Consumer {
		private String event;
		
		public AllEventListener(String event) throws SEPAProtocolException, SEPASecurityException {
			super(app, "EVENT");
			
			this.event = event;
		}

		//Variables: ?thing ?timeStamp OPTIONAL : ?value
		@Override
		public void onAddedResults(BindingsResults results) {
			HashSet<Event> ret = new HashSet<Event>();
			for(Bindings bindings : results.getBindings()) {
				bindings.getBindingValue("thing");
				ret.add(new Event(event,bindings.getBindingValue("thing"),bindings.getBindingValue("timeStamp"),bindings.getBindingValue("value")));
			}
			onEvent(ret);
		}

		@Override
		public void onRemovedResults(BindingsResults results) {
			
		}

		@Override
		public void onResults(ARBindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPing() {
			onConnectionStatus(true);
			
		}

		@Override
		public void onBrokenSocket() {
			onConnectionStatus(false);
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			onConnectionError(errorResponse);			
		}	
	}
	
	class ThingEventListener extends Consumer {
		private String thing;
		private String event;
		
		public ThingEventListener(String thing,String event) throws SEPAProtocolException, SEPASecurityException {
			super(app, "THING_EVENT");
			
			this.thing = thing;
			this.event = event;
		}

		@Override
		public void onResults(ARBindingsResults results) {
			
		}
		
		//Variables: ?timeStamp OPTIONAL : ?value
		@Override
		public void onAddedResults(BindingsResults results) {
			HashSet<Event> ret = new HashSet<Event>();
			for(Bindings bindings : results.getBindings()) {
				bindings.getBindingValue("thing");
				ret.add(new Event(thing,event,bindings.getBindingValue("timeStamp"),bindings.getBindingValue("value")));
			}
			onEvent(ret);	
		}

		@Override
		public void onRemovedResults(BindingsResults results) {
			
		}

		@Override
		public void onPing() {
			onConnectionStatus(true);
			
		}

		@Override
		public void onBrokenSocket() {
			onConnectionStatus(false);
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			onConnectionError(errorResponse);
			
		}
		
	}
	
	public EventListener() throws SEPAPropertiesException  {
		app = new ApplicationProfile("td.jsap");
	}

	public EventListener(ApplicationProfile app) {
		this.app = app;
	}
	
}
