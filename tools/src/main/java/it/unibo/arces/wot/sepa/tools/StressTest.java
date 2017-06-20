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
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;

public class StressTest {
	private static final Logger logger = LogManager.getLogger("StressTest");
	static int nConsumers = 10;
	static int nProducers = 20;
	static int producerUpdates = 10;

	static CountDownLatch publishingEnded = new CountDownLatch(nProducers * producerUpdates);
	static CountDownLatch notificationEnded = new CountDownLatch(nProducers * producerUpdates * nConsumers);

	private static HashMap<Integer, Float> meanNotificationPeriod = new HashMap<Integer, Float>();
	private static HashMap<Integer, Integer> notifications = new HashMap<Integer, Integer>();

	private static HashMap<Integer, Float> meanUpdatePeriod = new HashMap<Integer, Float>();
	private static HashMap<Integer, Integer> updates = new HashMap<Integer, Integer>();

	private static synchronized void notification(int index, float period) {
		Integer number;
		if ((number = notifications.get(index)) != null) {
			notifications.put(index, number + 1);
			float mean = (meanNotificationPeriod.get(index) * (notifications.get(index) - 1) + period)
					/ notifications.get(index);
			meanNotificationPeriod.put(index, mean);
		} else {
			notifications.put(index, 1);
			meanNotificationPeriod.put(index, period);
		}
		notificationEnded.countDown();
		logger.info("*** NOTIFICATION *** #"+index+" "+period+" ms ("+notificationEnded.getCount()+")");
	}

	private static synchronized void update(int index, float period) {
		Integer number;
		if ((number = updates.get(index)) != null) {
			updates.put(index, number + 1);
			float mean = (meanUpdatePeriod.get(index) * (updates.get(index) - 1) + period) / updates.get(index);
			meanUpdatePeriod.put(index, mean);
		} else {
			updates.put(index, 1);
			meanUpdatePeriod.put(index, period);
		}
		publishingEnded.countDown();
		logger.info("*** UPDATE *** #"+index+" "+period+" ms ("+publishingEnded.getCount()+")");
	}

	public static void main(String[] args)  {
		for (int i = 0; i < nConsumers; i++) {
			try {
				new Thread(new StressTest().new Subscriber("client.jpar", i)).start();
			} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException
					| NoSuchElementException | KeyStoreException | NoSuchAlgorithmException | CertificateException
					| InterruptedException | IOException | URISyntaxException | InvalidKeyException | NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
				
				e.printStackTrace();
				System.exit(1);
			}
		}

		for (int i = 0; i < nProducers; i++) {
			try {
				new Thread(new StressTest().new Publisher("client.jpar", i, producerUpdates)).start();
			} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException
					| NoSuchElementException | KeyStoreException | NoSuchAlgorithmException | CertificateException
					| IOException | InvalidKeyException | NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException e) {
				
				e.printStackTrace();
				System.exit(1);
			}
		}

		try {
			notificationEnded.await();
		} catch (InterruptedException e) {
		}

		logger.info("UPDATES:" + updates.toString());
		logger.info("NOTIFICATIONS:" + notifications.toString());
		logger.info("UPDATE PERIOD:" + meanUpdatePeriod.toString());
		logger.info("NOTIFICATION PERIOD:" + meanNotificationPeriod.toString());
		
		System.exit(0);
	}

	class Subscriber extends GenericClient implements Runnable {
		private Date previous = null;
		private int index = 0;

		public Subscriber(String jparFile, int i)
				throws InterruptedException, IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException, URISyntaxException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
			super(jparFile);
			index = i;
			previous = new Date();
			update("delete {?s ?p ?o} where {?s ?p ?o}",null);
			subscribe("select * where {?s ?p ?o}", null);
		}

		@Override
		public void run() {}

		@Override
		public void onResults(ARBindingsResults results) {
			Date now = new Date();
			float period = now.getTime() - previous.getTime();
			StressTest.notification(index, period);
			previous = now;
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUnsubscribe() {
			// TODO Auto-generated method stub
			
		}
	}

	class Publisher extends GenericClient implements Runnable {
		private int index = 0;
		public int nUpdate = 0;

		public Publisher(String jparFile, int i, int nu)
				throws IllegalArgumentException, FileNotFoundException, NoSuchElementException, IOException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, NullPointerException, ClassCastException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, URISyntaxException {
			super(jparFile);
			index = i;
			nUpdate = nu;
		}

		@Override
		public void run() {
			String id = UUID.randomUUID().toString();
			String UPDATE = "prefix test:<http://www.vaimee.com/test#> delete {?id test:value ?oldValue} insert {?id test:value ?value} where {OPTIONAL{?id test:value ?oldValue}}";
			Integer i;
			Bindings bindings = new Bindings();
			bindings.addBinding("id", new RDFTermURI("test:" + id));
			for (i = 0; i < nUpdate; i++) {
				bindings.addBinding("value", new RDFTermLiteral(i.toString()));
				Date start = new Date();
				update(UPDATE, bindings);
				Date stop = new Date();
				StressTest.update(index, stop.getTime() - start.getTime());
				/*try {
					Thread.sleep((long) (Math.random() * 1000));
				} catch (InterruptedException e) {
				}*/
			}
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
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onUnsubscribe() {
			// TODO Auto-generated method stub
			
		}
	}
}
