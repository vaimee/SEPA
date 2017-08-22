package it.unibo.arces.wot.sepa.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class HeapBugTest {
	
	private static int N_OBSERVATIONS = 500;
	private static int MIN_SLEEP = 1000;
	private static int DELTA_MAX_SLEEP = 1000;
	
	private static Producer creator;
	static ApplicationProfile app;
	
	static final String observation = "arces-monitor:observation";
	static final String comment = "Comment";
	static final String label = "Label";
	static final String location = "arces-monitor:location";
	static final String unit = "arces-monitor:unitFake";
	
	static ArrayList<HeapStressThread> threads = new ArrayList<HeapStressThread>();
	
	class HeapStressThread extends Thread {
		private Producer producer;
		private int index;
		
		public HeapStressThread(int index) throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
			this.index = index;
			producer = new Producer(app,"UPDATE_OBSERVATION_VALUE");
		}
		
		public void run() {
			Bindings fb = new Bindings();
			fb.addBinding("observation", new RDFTermURI(observation+index));
			while(true) {
				int n = (int) (MIN_SLEEP+DELTA_MAX_SLEEP*Math.random());
				try {
					Thread.sleep(n);
				} catch (InterruptedException e) {
					return;
				}
				fb.addBinding("value", new RDFTermLiteral(String.format("%d", n)));
				producer.update(fb);
			}
		}
	}
	
	public static void main(String[] args) throws InvalidKeyException, FileNotFoundException, NoSuchElementException, IllegalArgumentException, NullPointerException, ClassCastException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException, URISyntaxException {
		if (args.length != 3) {
			System.out.println("Usage: HeapBugTest <threads> <min sleep ms> <delta sleep ms>");
			System.exit(1);
		}
		
		N_OBSERVATIONS = Integer.parseInt(args[0]);
		MIN_SLEEP = Integer.parseInt(args[1]);
		DELTA_MAX_SLEEP = Integer.parseInt(args[2]);
		
		app = new ApplicationProfile("mqttMonitoring.jsap");
		
		creator = new Producer(app,"ADD_OBSERVATION");

		Bindings fb = new Bindings();
		fb.addBinding("unit", new RDFTermURI(unit));
		
		for (int i=0; i < N_OBSERVATIONS;i++) {
			fb.addBinding("observation", new RDFTermURI(observation+i));
			fb.addBinding("comment", new RDFTermLiteral(comment+i));
			fb.addBinding("label", new RDFTermLiteral(label+i));
			fb.addBinding("location", new RDFTermURI(location+i));
			creator.update(fb);
			HeapStressThread th = new HeapBugTest().new HeapStressThread(i);
			threads.add(th);
			th.start();
		}
		
		System.out.println("Press any key to exit...");
		System.in.read();
		
		for (HeapStressThread th : threads) th.interrupt();
		
	}
}
