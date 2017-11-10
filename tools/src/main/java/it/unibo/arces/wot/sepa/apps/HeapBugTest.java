package it.unibo.arces.wot.sepa.apps;

import java.io.IOException;
import java.util.ArrayList;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class HeapBugTest {

	private static int N_OBSERVATIONS = 10;
	private static int MIN_SLEEP = 500;
	private static int DELTA_MAX_SLEEP = 500;
	private static int N_CONSUMERS = 10;
	
	private static Producer creator;
	static ApplicationProfile app;

	static final String observation = "arces-monitor:observation";
	static final String comment = "Comment";
	static final String label = "Label";
	static final String location = "arces-monitor:location";
	static final String unit = "arces-monitor:unitFake";

	static ArrayList<HeapBugTestProducer> threads = new ArrayList<HeapBugTestProducer>();

	class HeapBugTestConsumer extends Consumer {
		private int number;
		private long notifications;
		
		public HeapBugTestConsumer(ApplicationProfile appProfile, String subscribeID,int number) throws SEPAProtocolException, SEPASecurityException {
			super(appProfile, subscribeID);
			this.number = number;
			notifications = 0;
		}

		@Override
		public void onResults(ARBindingsResults results) {
			notifications++;
			System.out.println("Consumer #"+number+" Notifications:"+notifications);
			
		}

		@Override
		public void onAddedResults(BindingsResults results) {

		}

		@Override
		public void onRemovedResults(BindingsResults results) {

		}

		@Override
		public void onPing() {
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
	
	class HeapBugTestProducer extends Thread {
		private Producer producer;
		private int index;
		private long number;
		
		public HeapBugTestProducer(int index)
				throws SEPAProtocolException, SEPASecurityException {
			this.index = index;
			producer = new Producer(app, "UPDATE_OBSERVATION_VALUE");
			number = 0;
		}

		public void run() {
			Bindings fb = new Bindings();
			fb.addBinding("observation", new RDFTermURI(observation + index));
			while (true) {
				int n = (int) (MIN_SLEEP + DELTA_MAX_SLEEP * Math.random());
				try {
					Thread.sleep(n);
				} catch (InterruptedException e) {
					return;
				}
				fb.addBinding("value", new RDFTermLiteral(String.format("%d", number++)));
				producer.update(fb);
			}
		}
	}

	public static void printUsage() {
		System.out.println("Usage: HeapBugTest [COMMANDS][OPTIONS]");
		System.out.println("COMMANDS:");
		System.out.println("-h to print this guide");
		System.out.println("-pro <producers> (number of producers, default: 10)");
		System.out.println("-con <consumers> (number of consumers, default: 10)");
		System.out.println("-min <min sleep ms> (minimun sleep time of producers, default 500)");
		System.out.println("-max <delta sleep ms> (delta sleep time of producers, default 500)");
		System.exit(1);
	}

	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {
		if (args.length > 1) {
			for (int index = 0; index < args.length; index++) {
				switch (args[index]) {
				case "-pro":
					index++;
					if (index < args.length) try{
						N_OBSERVATIONS = Integer.parseInt(args[index]);
						
					}catch(NumberFormatException e) {
						printUsage();
					}
					else printUsage();
					break;
				case "-con":
					index++;
					if (index < args.length) try{
						N_CONSUMERS = Integer.parseInt(args[index]);
						
					}catch(NumberFormatException e) {
						printUsage();
					}else printUsage();
					break;
				case "-min":
					index++;
					if (index <= args.length) try{
						MIN_SLEEP = Integer.parseInt(args[index]);
						
					}catch(NumberFormatException e) {
						printUsage();
					}else printUsage();
					break;
				case "-max":
					index++;
					if (index <= args.length) try{
						DELTA_MAX_SLEEP = Integer.parseInt(args[index]);	
					}catch(NumberFormatException e) {
						printUsage();
					}else printUsage();
					break;
				}
			}
		} else if (args.length == 1) {
			if (!args[0].equals("-default"))
				printUsage();
		} else
			printUsage();

		app = new ApplicationProfile("mqttMonitoring.jsap");

		System.out.println("Context creation");
		creator = new Producer(app, "ADD_OBSERVATION");

		System.out.println("Consumers creation");
		for (int i=0; i < N_CONSUMERS;i++) {
			HeapBugTestConsumer consumer = new HeapBugTest().new HeapBugTestConsumer(app,"OBSERVATIONS",i);
			consumer.subscribe(null);
		}
		
		Bindings fb = new Bindings();
		fb.addBinding("unit", new RDFTermURI(unit));

		System.out.println("Producers creation");
		for (int i = 0; i < N_OBSERVATIONS; i++) {
			fb.addBinding("observation", new RDFTermURI(observation + i));
			fb.addBinding("comment", new RDFTermLiteral(comment + i));
			fb.addBinding("label", new RDFTermLiteral(label + i));
			fb.addBinding("location", new RDFTermURI(location + i));
			creator.update(fb);
			HeapBugTestProducer th = new HeapBugTest().new HeapBugTestProducer(i);
			threads.add(th);
			th.start();
		}

		System.out.println("Press any key to exit...");
		System.in.read();

		for (HeapBugTestProducer th : threads)
			th.interrupt();

	}
}
