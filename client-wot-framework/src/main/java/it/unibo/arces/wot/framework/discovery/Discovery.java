package it.unibo.arces.wot.framework.discovery;

import java.util.HashSet;
import java.util.Observable;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;

public class Discovery extends Observable {
	
	private ApplicationProfile app;
	
	private GetAllThings getAllThings;
	
	public enum DiscoveryEventType {THINGS,EVENTS,ACTIONS,PROPERTIES};
	
	public class DiscoveryEvent {
		private DiscoveryEventType type;
		private HashSet<Discoverable> results;
		private boolean added;
		
		public DiscoveryEvent(DiscoveryEventType type,HashSet<Discoverable> results,boolean added) {
			this.type = type;
			this.results = results;
			this.added = added;
		}
		
		public DiscoveryEventType getType(){
			return type;
		}
		
		public HashSet<Discoverable> getResults(){
			return results;
		}
		
		public boolean isAdded() {
			return added;
		}
		
		public boolean isRemoved() {
			return !added;
		}
	}
	
	public Discovery() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		this(new ApplicationProfile("td.jsap"));
	}
	
	public Discovery(ApplicationProfile app) throws SEPAProtocolException, SEPASecurityException{
		this.app = app;
		
		getAllThings = new GetAllThings();
	}
	
	public void enableAllThingsDiscovery() {
		getAllThings.subscribe(null);
	}
	
	public void disableAllThingsDiscovery() {
		getAllThings.unsubscribe();
	}
	
	class GetAllThings extends Consumer {

		public GetAllThings() throws SEPAProtocolException, SEPASecurityException {
			super(app, "ALL_THINGS");
		}

		@Override
		public void onResults(ARBindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAddedResults(BindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onRemovedResults(BindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onBrokenSocket() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			// TODO Auto-generated method stub
			
		}		
	}
}
