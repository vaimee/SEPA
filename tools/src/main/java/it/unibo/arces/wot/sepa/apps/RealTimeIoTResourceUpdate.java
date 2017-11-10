package it.unibo.arces.wot.sepa.apps;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

public class RealTimeIoTResourceUpdate {
	protected static final Logger logger = LogManager.getLogger("RealTimeIoTResourceUpdate");
	
	private static int nThreads = 1;
	private static int nUpdates = 1000;
	
	private static ExecutorService producers = Executors.newFixedThreadPool(nThreads);
	
	public class ProducerThread extends Producer implements Runnable {
		
		public ProducerThread(ApplicationProfile appProfile, String updateID) throws SEPAProtocolException, SEPASecurityException{
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
	
	public static void main(String[] args) throws SEPAPropertiesException, SEPAProtocolException, SEPASecurityException  {
		ApplicationProfile app = null;
		
		app = new ApplicationProfile("sapexamples/GatewayProfile.jsap");
		
		
		for (int i=0; i < nThreads; i++) {
			producers.execute(new RealTimeIoTResourceUpdate().new ProducerThread(app,"UPDATE_RESOURCE"));
			
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
