package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

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
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class StatusMonitor extends Aggregator implements Runnable{
	protected static final Logger logger = LogManager.getLogger("StatusMonitor");
	
	private static ApplicationProfile app;
	
	private class ThingStatus {
		private boolean status = true;
		private boolean ping = true;
		
		public boolean isOn() {
			return status;
		}
		public void setOn() {
			this.status = true;
		}
		public void setOff() {
			this.status = false;
		}
		public boolean pingReceived() {
			return ping;
		}
		public void setPing() {
			this.ping = true;
		}
		public void resetPing() {
			this.ping = false;
		}	
	}
	
	private ConcurrentHashMap<String,ThingStatus> status = new ConcurrentHashMap<String,ThingStatus>();
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException, InterruptedException { 
		app = new ApplicationProfile("td.jsap");
	
		 new Thread(new StatusMonitor()).start();
	}
	
	public StatusMonitor()
			throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException {
		super(app, "EVENT", "UPDATE_DISCOVER");
		
		Bindings bindings = new Bindings();
		bindings.addBinding("event", new RDFTermURI("wot:Ping"));
		subscribe(bindings);
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			String thing = bindings.getBindingValue("thing");
			
			status.put(thing, new ThingStatus());
			
			logger.info("Ping received by Web Thing: "+thing);
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
	}

	@Override
	public void onSubscribe(BindingsResults results) {
	}

	@Override
	public void onUnsubscribe() {
	}

	@Override
	public void run() {
		while(true) {
			try {
				Thread.sleep(15000);
				logger.info("Check Web Things status...next check in 15 secs...");
			} catch (InterruptedException e) {
				
			}
			
			for (String thing : status.keySet()) {
				if (!status.get(thing).pingReceived()) {
					if (status.get(thing).isOn()) {
						//Turn off and set as not discoverable
						status.remove(thing);
						
						//Update
						Bindings bindings = new Bindings();
						bindings.addBinding("value", new RDFTermLiteral("false"));
						bindings.addBinding("thing", new RDFTermURI(thing));
						update(bindings);
						
						logger.warn("Turn OFF Web Thing: "+thing);
					}
				}
				else {
					if (!status.get(thing).isOn()) {
						//Turn on and set as discoverable
						status.get(thing).setOn();
						
						//Update
						Bindings bindings = new Bindings();
						bindings.addBinding("value", new RDFTermLiteral("true"));
						bindings.addBinding("thing", new RDFTermURI(thing));
						update(bindings);
						
						logger.info("Turn ON Web Thing: "+thing);
					}
				}
				
				status.get(thing).resetPing();
			}
		}
		
	}
	
}
