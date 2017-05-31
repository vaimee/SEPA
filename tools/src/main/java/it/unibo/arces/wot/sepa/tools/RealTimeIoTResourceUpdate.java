package it.unibo.arces.wot.sepa.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

public class RealTimeIoTResourceUpdate {
	protected static final Logger logger = LogManager.getLogger("RealTimeIoTResourceUpdate");
	
	private static int nThreads = 1;
	private static int nUpdates = 1000;
	
	private static ExecutorService producers = Executors.newFixedThreadPool(nThreads);
	
	public class ProducerThread extends Producer implements Runnable {
		
		public ProducerThread(ApplicationProfile appProfile, String updateID) {
			super(appProfile, updateID);
		}

		@Override
		public void run() {
			int i = 0;
			Bindings bindings = new Bindings();
			bindings.addBinding("resource", new RDFTermURI("iot:Resource_"+UUID.randomUUID().toString()));
			while (i++ < nUpdates) {
				double value = Math.random()*100;
				bindings.addBinding("value", new RDFTermLiteral(String.format("%.2f", value)));
				update(bindings);
			}
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException {
		ApplicationProfile app = new ApplicationProfile("sapexamples/GatewayProfile.jsap");
		
		///Thread th = null;
		
		for (int i=0; i < nThreads; i++) {
			producers.execute(new RealTimeIoTResourceUpdate().new ProducerThread(app,"UPDATE_RESOURCE"));
			//th = new Thread(new RealTimeIoTResourceUpdate().new ProducerThread(app,"UPDATE_RESOURCE"));
			//th.setName("S"+i);
			//th.start();
		}
		/*			
		synchronized(th){
			try {
				th.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		try {
			producers.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn(e.getMessage());
		}
	}
}
