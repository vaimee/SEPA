package it.unibo.arces.wot.sepa.pattern;

import org.junit.jupiter.api.*;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.logging.Logging;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

public class ITPattern {
	protected static ConfigurationProvider provider;

	protected static ConsumerTestUnit consumerAll;
	protected static ConsumerTestUnit consumerRandom1;
	protected static Producer randomProducer;
	protected static AggregatorTestUnit randomAggregator;
	protected static GenericClient genericClient;
	protected static Producer deleteAll;
	
	private static Sync handler;

	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		provider = new ConfigurationProvider();
		
		handler = new Sync();
		handler.reset();
		
		genericClient = new GenericClient(provider.getJsap(), handler);
		
		consumerAll = new ConsumerTestUnit(provider, "ALL",handler);
		randomProducer = new Producer(provider.getJsap(), "RANDOM");
		consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1",handler);
		randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1",handler);	
		deleteAll = new Producer(provider.getJsap(), "DELETE_ALL");
	}

	@AfterAll
	public static void end() throws InterruptedException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, IOException {	
		consumerAll.close();
		randomAggregator.close();
		consumerRandom1.close();
		randomProducer.close();
		genericClient.close();
		deleteAll.close();
	}

	@BeforeEach
	public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, InterruptedException, SEPABindingsException {	
		Thread.sleep(ConfigurationProvider.SLEEP);
		assertFalse(handler.getSubscribes() != handler.getUnsubscribes(), "Subscribes: "+handler.getSubscribes()+ " Unsubscribe: "+handler.getUnsubscribes());
		
		handler.reset();
		
		Response ret =randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
	}

	@AfterEach
	public void afterTest() throws IOException, SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, InterruptedException {
		assertFalse(handler.getSubscribes() != handler.getUnsubscribes(), "Subscribes: "+handler.getSubscribes()+ " Unsubscribe: "+handler.getUnsubscribes());
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void subscribe() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerAll.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void produce() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		
		Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void subscribeAndResults() throws InterruptedException, SEPASecurityException, IOException,
			SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {

		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		consumerAll.waitFirstNotification();
		consumerAll.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void notification() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		
		consumerAll.syncSubscribe(provider.TIMEOUT, provider.NRETRY);

		Response ret =randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
		
		consumerAll.waitNotification();
		consumerAll.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Disabled
	@Timeout(10)
	public void aggregation() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		Logging.logger.debug("Aggregator");
		
		consumerRandom1.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		Logging.logger.debug("Aggregator first subscribe ok");

		
		randomAggregator.syncSubscribe(provider.TIMEOUT, provider.NRETRY);
		Logging.logger.debug("Aggregator second subscribe ok");

		Response ret = randomProducer.update(provider.TIMEOUT, provider.NRETRY);
		assertFalse(ret.isError(),ret.toString());
		Logging.logger.debug("Aggregator Update Done");

		randomAggregator.waitNotification();
		consumerRandom1.waitNotification();
		Logging.logger.debug("Aggregator stop");
		
		consumerRandom1.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
		randomAggregator.syncUnsubscribe(provider.TIMEOUT, provider.NRETRY);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void genericClientSingleSubscribe() {
		try {
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
