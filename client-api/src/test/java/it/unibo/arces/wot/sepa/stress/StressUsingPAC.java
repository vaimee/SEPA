package it.unibo.arces.wot.sepa.stress;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.Producer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.logging.Logging;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;

public class StressUsingPAC implements ISubscriptionHandler {
	static ConfigurationProvider provider;

	protected static ConsumerTestUnit consumerAll;
	protected static Producer randomProducer;
	protected static Producer hugeProducer;
	protected static Producer deleteAll;
	protected static AggregatorTestUnit randomAggregator;
	protected static ConsumerTestUnit consumerRandom1;
	protected static ConsumerTestUnit consumerHuge;

	protected static GenericClient genericClient;
	protected static HashMap<String, String> subscriptions = new HashMap<>();

	private int genericClientNotifications;
	private int genericClientSubscriptions;

	public void setOnSemanticEvent(String spuid) {
		genericClientNotifications++;
	}

	public int getNotificationsCount() {
		return genericClientNotifications;
	}

	public void setOnSubscribe(String spuid, String alias) {
		genericClientSubscriptions++;
	}

	public int getSubscriptionsCount() {
		return genericClientSubscriptions;
	}

	public void setOnUnsubscribe(String spuid) {
		genericClientSubscriptions--;
	}

	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		provider = new ConfigurationProvider();
	}

	@AfterAll
	public static void end() {
	}

	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		consumerAll = new ConsumerTestUnit(provider, "ALL");
		randomProducer = new Producer(provider.getJsap(), "RANDOM");
		hugeProducer = new Producer(provider.getJsap(), "HUGE");
		randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1");
		consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1");
		genericClient = new GenericClient(provider.getJsap(), this);
		consumerHuge = new ConsumerTestUnit(provider, "HUGE");
		deleteAll = new Producer(provider.getJsap(), "DELETE_ALL");
	}
	

	@AfterEach
	public void afterTest() throws IOException {
		consumerAll.close();
		randomProducer.close();
		randomAggregator.close();
		consumerRandom1.close();
		consumerHuge.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	// (timeout = 5000)
	public void produceX100() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		for (int i = 0; i < 100; i++) {
			Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	// @Timeout(60)
	public void produceX1000() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		for (int i = 0; i < 1000; i++) {
			Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void aggregationX10() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerRandom1.waitFirstNotification();

		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		randomAggregator.waitFirstNotification();

		for (int i = 0; i < 10; i++) {
			randomProducer.update(provider.TIMEOUT, provider.NRETRY);

			randomAggregator.waitNotification();
			consumerRandom1.waitNotification();
		}
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(60)
	public void hugex1000() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		for (int i = 0; i < 1000; i++) {
			hugeProducer.update(provider.TIMEOUT, provider.NRETRY);
		}
		
		consumerHuge.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerHuge.waitFirstNotification();
		consumerHuge.unsubscribe();
		
		deleteAll.update();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	// @Timeout(10)
	public void subscribeAndUpdateRaceDetection() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		Thread pub = new Thread() {
			public void run() {
				for (int i = 0; i < 500; i++) {
					try {
						randomProducer.update(provider.TIMEOUT, provider.NRETRY);
					} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException
							| SEPAProtocolException e) {
						assertFalse(true, "Failed on update: " + i + " " + e.getMessage());
					}
				}
			}
		};

		Thread sub = new Thread() {
			public void run() {
				for (int i = 0; i < 500; i++) {
					try {
						consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1");
						consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerRandom1.close();
						
						consumerAll = new ConsumerTestUnit(provider, "ALL");
						consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
						consumerAll.close();
					} catch (SEPASecurityException | SEPAPropertiesException | SEPABindingsException
							| SEPAProtocolException | IOException | InterruptedException e) {
						assertFalse(true, "Failed on subscribe: " + i + " " + e.getMessage());
					}
				}
			}
		};

		pub.start();
		sub.start();

		synchronized (pub) {
			pub.wait();
		}
		synchronized (sub) {
			sub.wait();
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(30)
	// (timeout = 10000)
	public void aggregationX100() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerRandom1.waitFirstNotification();

		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		randomAggregator.waitFirstNotification();

		for (int i = 0; i < 100; i++) {
			randomProducer.update(provider.TIMEOUT, provider.NRETRY);

			randomAggregator.waitNotification();
			consumerRandom1.waitNotification();
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		Logging.logger.debug(notify);
		setOnSemanticEvent(notify.getSpuid());
	}

	@Override
	public void onBrokenConnection(ErrorResponse err) {
		Logging.logger.debug("onBrokenConnection " + err);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.logger.debug(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		Logging.logger.debug("onSubscribe " + spuid + " " + alias);
		subscriptions.put(alias, spuid);
		setOnSubscribe(spuid, alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		Logging.logger.debug("onUnsubscribe " + spuid);
		setOnUnsubscribe(spuid);
	}
}
