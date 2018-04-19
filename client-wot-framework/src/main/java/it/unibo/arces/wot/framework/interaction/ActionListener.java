package it.unibo.arces.wot.framework.interaction;

import java.util.HashMap;

import it.unibo.arces.wot.framework.elements.Action;
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

public abstract class ActionListener {
	private ApplicationProfile app;
	
	private HashMap<String,WoTActionListener> actionListener = new HashMap<String,WoTActionListener>();
	
	public abstract void onAction(Action action);
	public abstract void onConnectionStatus(Boolean on);
	public abstract void onConnectionError(ErrorResponse error);
	
	private class WoTActionListener extends Consumer {

		public WoTActionListener() throws SEPAProtocolException, SEPASecurityException {
			super(app, "ACTION");
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
	
	public void startListeningForAction(String actionURI) throws SEPAProtocolException, SEPASecurityException  {
		if (actionListener.containsKey(actionURI)) return;
		WoTActionListener listener = new WoTActionListener();
		Bindings bindings = new Bindings();
		bindings.addBinding("action", new RDFTermURI(actionURI));
		listener.subscribe(bindings);
		actionListener.put(actionURI, listener);
	}
	
	public void stopListeningForAction(String actionURI)  {
		if (!actionListener.containsKey(actionURI)) return;
		actionListener.get(actionURI).unsubscribe();
		actionListener.remove(actionURI);
	}
	
	public ActionListener() throws SEPAPropertiesException  {	
		app = new ApplicationProfile("td.jsap");
	}
	
	public ActionListener(ApplicationProfile app) {	
		this.app = app; 
	}
}
