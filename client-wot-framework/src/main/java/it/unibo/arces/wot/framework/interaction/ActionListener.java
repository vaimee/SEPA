package it.unibo.arces.wot.framework.interaction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.framework.elements.Action;
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

		public WoTActionListener()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "ACTION");
		}

		@Override
		public void onResults(ARBindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onAddedResults(BindingsResults results) {
			//TODO to be implemented
			
		}

		@Override
		public void onRemovedResults(BindingsResults results) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onKeepAlive() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onBrokenSubscription() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onSubscriptionError(ErrorResponse errorResponse) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public void startListeningForAction(String actionURI) throws InvalidKeyException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException, URISyntaxException, InterruptedException {
		if (actionListener.containsKey(actionURI)) return;
		WoTActionListener listener = new WoTActionListener();
		Bindings bindings = new Bindings();
		bindings.addBinding("action", new RDFTermURI(actionURI));
		listener.subscribe(bindings);
		actionListener.put(actionURI, listener);
	}
	
	public void stopListeningForAction(String actionURI) throws InvalidKeyException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException, URISyntaxException, InterruptedException {
		if (!actionListener.containsKey(actionURI)) return;
		actionListener.get(actionURI).unsubscribe();
		actionListener.remove(actionURI);
	}
	
	public ActionListener() throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {	
		app = new ApplicationProfile("td.jsap");
	}
	
	public ActionListener(ApplicationProfile app) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {	
		this.app = app; 
	}
}
