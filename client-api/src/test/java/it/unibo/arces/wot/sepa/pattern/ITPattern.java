package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;

public class ITPattern implements ISubscriptionHandler{

	protected static final Logger logger = LogManager.getLogger();
	
	protected static JSAP app = null;
	protected static ConfigurationProvider provider;
	protected static ClientSecurityManager sm = null;

	protected static ConsumerTestUnit consumerAll;
	protected static Producer randomProducer;
	protected static AggregatorTestUnit randomAggregator;
	protected static ConsumerTestUnit consumerRandom1;
	
	protected static GenericClient genericClient;
	protected static HashMap<String,String> subscriptions = new HashMap<>();
	
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
		try {
			provider = new ConfigurationProvider();
			app = provider.getJsap();
		} catch (SEPAPropertiesException | SEPASecurityException e) {
			assertFalse(true,"Configuration not found");
		}

		if (app.isSecure()) {
			sm = provider.getSecurityManager();
			Response ret = sm.register("SEPATest");
			ret = sm.refreshToken();
			assertFalse(ret.isError());
		}
	}
	
	@AfterAll
	public static void end() {
		logger.debug("end");
	}
	
	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		consumerAll = new ConsumerTestUnit(app, "ALL", sm);
		randomProducer = new Producer(app, "RANDOM", sm);
		randomAggregator = new AggregatorTestUnit(app, "RANDOM", "RANDOM1", sm);
		consumerRandom1 = new ConsumerTestUnit(app, "RANDOM1", sm);
		
		genericClient = new GenericClient(app, sm, this);
		
		genericClientNotifications = 0;
		genericClientSubscriptions = 0;
		subscriptions = new HashMap<>();
	}

	@AfterEach
	public void afterTest() throws IOException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		consumerAll.unsubscribe(provider.TIMEOUT,provider.NRETRY);
		consumerAll.close();
		
		randomAggregator.unsubscribe(provider.TIMEOUT,provider.NRETRY);
		randomAggregator.close();
		
		consumerRandom1.unsubscribe(provider.TIMEOUT,provider.NRETRY);
		consumerRandom1.close();
		
		randomProducer.close();
		
		genericClient.close();
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void subscribe() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void produce() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		Response ret = randomProducer.update(provider.TIMEOUT,provider.NRETRY);
		
		assertFalse(ret.isError());
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void subscribeAndResults() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
		consumerAll.waitFirstNotification();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void notification() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe(provider.TIMEOUT,provider.NRETRY);

		randomProducer.update(provider.TIMEOUT,provider.NRETRY);

		consumerAll.waitNotification();
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void aggregation() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {		
		logger.debug("Aggregator");
		consumerRandom1.syncSubscribe(provider.TIMEOUT,provider.NRETRY);

		logger.debug("Aggregator first subscribe ok");

		randomAggregator.syncSubscribe(provider.TIMEOUT,provider.NRETRY);

		logger.debug("Aggregator second subscribe ok");

		randomProducer.update(provider.TIMEOUT,provider.NRETRY);
		logger.debug("Aggregator Update Done");

		randomAggregator.waitNotification();
		consumerRandom1.waitNotification();
		logger.debug("Aggregator stop");
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void genericClientSingleSubscribe() {
		try {
			genericClient.subscribe("ALL", null, "first",provider.TIMEOUT,provider.NRETRY);
			
			if (getSubscriptionsCount() != 1) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=1,"Failed to subscribe");
			}
			
			genericClient.update("RANDOM", null,provider.TIMEOUT,provider.NRETRY);
			
			if (getNotificationsCount() != 2) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getNotificationsCount()!=2,"Failed to notify");
			}
			
			genericClient.unsubscribe(subscriptions.get("first"),provider.TIMEOUT,provider.NRETRY);
			
			if (getSubscriptionsCount() != 0) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=0,"Failed to unsubscribe");
			}
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true,e.getMessage());
		}
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(5)
	public void genericClientDoubleSubscribe() {
		try {
			genericClient.subscribe("RANDOM", null, "first",provider.TIMEOUT,provider.NRETRY);
			genericClient.subscribe("RANDOM1", null, "second",provider.TIMEOUT,provider.NRETRY);
			
			if (getSubscriptionsCount() != 2) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=2,"Failed to subscribe");
			}
			
			genericClient.update("RANDOM", null,provider.TIMEOUT,provider.NRETRY);
			genericClient.update("RANDOM1", null,provider.TIMEOUT,provider.NRETRY);
			
			if (getNotificationsCount() != 4) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getNotificationsCount()!=2,"Failed to notify");
			}
			
			genericClient.unsubscribe(subscriptions.get("first"),provider.TIMEOUT,provider.NRETRY);
			genericClient.unsubscribe(subscriptions.get("second"),provider.TIMEOUT,provider.NRETRY);
					
			if (getSubscriptionsCount() != 0) {
				synchronized(this) {
					wait(1000);
				}
				assertFalse(getSubscriptionsCount()!=0,"Failed to unsubscribe");
			}
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true,e.getMessage());
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		logger.debug(notify);
		setOnSemanticEvent(notify.getSpuid());
	}

	@Override
	public void onBrokenConnection(ErrorResponse err) {
		logger.debug("onBrokenConnection "+err);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.debug(errorResponse);
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		logger.debug("onSubscribe "+spuid+" "+alias);
		subscriptions.put(alias, spuid);
		setOnSubscribe(spuid,alias);
	}

	@Override
	public void onUnsubscribe(String spuid) {
		logger.debug("onUnsubscribe "+spuid);
		setOnUnsubscribe(spuid);
	}
}
