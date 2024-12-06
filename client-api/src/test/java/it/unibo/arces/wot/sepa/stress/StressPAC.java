package it.unibo.arces.wot.sepa.stress;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.pattern.Producer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

public class StressPAC {
	static ConfigurationProvider provider;
	static Sync sync = new Sync();
	
	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		provider = new ConfigurationProvider();
	}

	@AfterAll
	public static void end() {
		assertFalse(sync.getSubscribes() != sync.getUnsubscribes(), "Subscribes: "+sync.getSubscribes()+"Unsubscribes: "+sync.getUnsubscribes());
	}

	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException, InterruptedException, IOException {
		Thread.sleep(ConfigurationProvider.SLEEP);
		
		sync.reset();
		
		assertFalse(sync.getSubscribes() != sync.getUnsubscribes(), "Subscribes: "+sync.getSubscribes()+"  == Unsubscribes: "+sync.getUnsubscribes());
		
		Producer deleteAll = new Producer(provider.getJsap(), "DELETE_ALL");
		deleteAll.update();
		deleteAll.close();
	}
	

	@AfterEach
	public void afterTest() throws IOException, InterruptedException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		assertFalse(sync.getSubscribes() != sync.getUnsubscribes(), "Subscribes: "+sync.getSubscribes()+" == Unsubscribes: "+sync.getUnsubscribes());
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void aggregationX10() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		ConsumerTestUnit consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1",sync);
		AggregatorTestUnit randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1",sync);
		Producer randomProducer = new Producer(provider.getJsap(), "RANDOM");
		
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		
		consumerRandom1.waitFirstNotification();
		randomAggregator.waitFirstNotification();
		
		assertFalse(sync.getSubscribes() != 2, "Active subscriptions (2): "+sync.getSubscribes());
		
		for (int i = 0; i < 10; i++) {
			randomProducer.update(provider.TIMEOUT, provider.NRETRY);

			randomAggregator.waitNotification();
			consumerRandom1.waitNotification();
		}
		
		assertFalse(sync.getEvents() != 20, "Total events (20): "+sync.getEvents());
			
		randomAggregator.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerRandom1.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		
		assertFalse(sync.getSubscribes() != sync.getUnsubscribes(), "Subscribes: "+sync.getSubscribes()+" == Unsubscribes: "+sync.getUnsubscribes());
		
		randomAggregator.close();
		consumerRandom1.close();
		randomProducer.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(30)
	public void aggregationX100() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		
		Producer randomProducer = new Producer(provider.getJsap(), "RANDOM");
		AggregatorTestUnit randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1",sync);
		ConsumerTestUnit consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1",sync);
		
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerRandom1.waitFirstNotification();

		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		randomAggregator.waitFirstNotification();
		
		assertFalse(sync.getSubscribes() != 2, "Active subscriptions (2): "+sync.getSubscribes());

		for (int i = 0; i < 100; i++) {
			randomProducer.update(provider.TIMEOUT, provider.NRETRY);

			randomAggregator.waitNotification();
			consumerRandom1.waitNotification();
		}
		
		assertFalse(sync.getEvents() != 200, "Total events (200): "+sync.getEvents());
		
		randomAggregator.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerRandom1.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		
		assertFalse(sync.getSubscribes() != sync.getUnsubscribes(), "Subscribes: "+sync.getSubscribes()+" == Unsubscribes: "+sync.getUnsubscribes());
		
		randomAggregator.close();
		consumerRandom1.close();
		randomProducer.close();
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(60)
	public void hugex100() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		
		Producer hugeProducer = new Producer(provider.getJsap(), "HUGE");
		ConsumerTestUnit consumerHuge = new ConsumerTestUnit(provider, "HUGE",sync);
		
		for (int i = 0; i < 100; i++) {
			hugeProducer.update(provider.TIMEOUT, provider.NRETRY);
		}
		
		consumerHuge.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerHuge.waitFirstNotification();
		
		consumerHuge.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerHuge.close();
		
		hugeProducer.close();
	}
	
	class Runner extends Thread {
		boolean run = true;
		public void run() {
			Producer randomProducer = null;
			try {
				randomProducer = new Producer(provider.getJsap(), "RANDOM");
			} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException e) {
				assertFalse(true, "Failed on create: " +e.getMessage());
			}
			for (int i=0; i < 500; i++) {
				try {
					randomProducer.update(provider.TIMEOUT, provider.NRETRY);
				} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException
						| SEPAProtocolException e) {
					assertFalse(true, "Failed on update: " + i + " "+e.getMessage());
				}
			}
			try {
				randomProducer.close();
			} catch (IOException e) {
				assertFalse(true, "Failed on close: " +e.getMessage());
			}
			if (!run) return;
		}
		
		public void terminate() {
			run = false;
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(120)
	public void subscribeAndUpdateRaceDetection() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		Runner pub = new Runner();

		Thread sub = new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					try {
						ConsumerTestUnit consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1",sync);
						consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerRandom1.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerRandom1.close();
					} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException
							| SEPAProtocolException | IOException | InterruptedException e) {
						assertFalse(true, "Failed on subscribe: " + i + " " + e.getMessage());
					}
				}
			}
		};
		
		Thread sub1 = new Thread() {
			public void run() {
				for (int i = 0; i < 100; i++) {
					try {
						ConsumerTestUnit consumerAll = new ConsumerTestUnit(provider, "ALL",sync);
						consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerAll.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerAll.close();
					} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException
							| SEPAProtocolException | IOException | InterruptedException e) {
						assertFalse(true, "Failed on subscribe: " + i + " " + e.getMessage());
					}
				}
			}
		};

		sub.start();
		pub.start();
		sub1.start();
	
		pub.join();		
		sub.join();
		sub1.join();
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(10)
	public void produceX100() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		Producer randomProducer = new Producer(provider.getJsap(), "RANDOM");
		for (int i = 0; i < 100; i++) {
			Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}
		randomProducer.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	//@Timeout(10)
	public void produceX1000() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		Producer randomProducer = new Producer(provider.getJsap(), "RANDOM");
		for (int i = 0; i < 1000; i++) {
			Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}
		randomProducer.close();
	}
}
