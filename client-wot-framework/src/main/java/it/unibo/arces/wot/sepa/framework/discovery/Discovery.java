package it.unibo.arces.wot.sepa.framework.discovery;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Observable;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
	
	public Discovery() throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException{
		app = new ApplicationProfile("td.jsap");
		
		getAllThings = new GetAllThings();
	}
	
	public void enableAllThingsDiscovery() throws InvalidKeyException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException, URISyntaxException, InterruptedException {
		getAllThings.subscribe(null);
	}
	
	public void disableAllThingsDiscovery() throws InvalidKeyException, UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException, URISyntaxException, InterruptedException {
		getAllThings.unsubscribe();
	}
	
	class GetAllThings extends Consumer {

		public GetAllThings()
				throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
				NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			super(app, "GET_ALL_THINGS");
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
		public void onSubscribe(BindingsResults results) {
			onAddedResults(results);
		}

		@Override
		public void onUnsubscribe() {
			// TODO Auto-generated method stub
			
		}
		
	}
}
