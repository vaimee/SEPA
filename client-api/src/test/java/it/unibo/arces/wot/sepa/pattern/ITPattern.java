package it.unibo.arces.wot.sepa.pattern;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.Sync;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

public class ITPattern {

	protected static final Logger logger = LogManager.getLogger();

	protected static ConfigurationProvider provider;

	protected static ConsumerTestUnit consumerAll;
	protected static ConsumerTestUnit consumerRandom1;
	protected static Producer randomProducer;
	protected static AggregatorTestUnit randomAggregator;
	protected static GenericClient genericClient;
	
	private static Sync handler;
	private static ClientSecurityManager sm;

	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		provider = new ConfigurationProvider();
	}

	@AfterAll
	public static void end() {
	}

	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		sm = provider.buildSecurityManager();
		handler = new Sync(sm);

		consumerAll = null;
		randomProducer = null;
		randomAggregator = null;
		consumerRandom1 = null;

		genericClient = null ;
	}

	@AfterEach
	public void afterTest() throws IOException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		if (consumerAll != null) {
			consumerAll.unsubscribe(provider.TIMEOUT, provider.NRETRY);
			consumerAll.close();
		}

		if (randomAggregator != null) {
			randomAggregator.unsubscribe(provider.TIMEOUT, provider.NRETRY);
			randomAggregator.close();
		}

		if (consumerRandom1 != null) {
			consumerRandom1.unsubscribe(provider.TIMEOUT, provider.NRETRY);
			consumerRandom1.close();
		}

		if (randomProducer != null)
			randomProducer.close();

		if (genericClient != null)	genericClient.close();
		
		sm.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void subscribe() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll = new ConsumerTestUnit(provider, "ALL",sm);

		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void produce() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		randomProducer = new Producer(provider.getJsap(), "RANDOM", sm);
		Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void subscribeAndResults() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
		consumerAll = new ConsumerTestUnit(provider, "ALL",sm);

		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerAll.waitFirstNotification();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void notification() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll = new ConsumerTestUnit(provider, "ALL",sm);
		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);

		randomProducer = new Producer(provider.getJsap(), "RANDOM", sm);
		Response ret =randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
		
		consumerAll.waitNotification();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void aggregation() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		logger.debug("Aggregator");
		consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1",sm);
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		logger.debug("Aggregator first subscribe ok");

		randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1",sm);
		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		logger.debug("Aggregator second subscribe ok");

		randomProducer = new Producer(provider.getJsap(), "RANDOM", sm);
		Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
		logger.debug("Aggregator Update Done");

		randomAggregator.waitNotification();
		consumerRandom1.waitNotification();
		logger.debug("Aggregator stop");
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void genericClientSingleSubscribe() {
		try {
			genericClient = new GenericClient(provider.getJsap(), sm, handler);
			genericClient.subscribe("ALL", null, "first", provider.TIMEOUT, provider.NRETRY);

			handler.waitSubscribes(1);

			Response ret = genericClient.update("RANDOM", null, provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(),ret.toString());
			
			handler.waitEvents(2);

			genericClient.unsubscribe(handler.getSpuid("first"), provider.TIMEOUT, provider.NRETRY);

			handler.waitUnsubscribes(1);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true, e.getMessage());
		}
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void genericClientDoubleSubscribe() {
		try {
			genericClient = new GenericClient(provider.getJsap(), sm, handler);
			genericClient.subscribe("RANDOM", null, "first", provider.TIMEOUT, provider.NRETRY);
			genericClient.subscribe("RANDOM1", null, "second", provider.TIMEOUT, provider.NRETRY);

			handler.waitSubscribes(2);

			Response ret = genericClient.update("RANDOM", null, provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(),ret.toString());
			ret =  genericClient.update("RANDOM1", null, provider.TIMEOUT, provider.NRETRY);
			assertFalse(ret.isError(),ret.toString());
			
			handler.waitEvents(4);

			genericClient.unsubscribe(handler.getSpuid("first"), provider.TIMEOUT, provider.NRETRY);
			genericClient.unsubscribe(handler.getSpuid("second"), provider.TIMEOUT, provider.NRETRY);

			handler.waitUnsubscribes(2);
		} catch (SEPAProtocolException | SEPASecurityException | SEPAPropertiesException | SEPABindingsException
				| InterruptedException | IOException e) {
			e.printStackTrace();
			assertFalse(true, e.getMessage());
		}
	}
}
