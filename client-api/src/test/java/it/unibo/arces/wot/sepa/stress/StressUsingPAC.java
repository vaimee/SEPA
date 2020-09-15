package it.unibo.arces.wot.sepa.stress;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.Producer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.Response;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashMap;

public class StressUsingPAC  implements ISubscriptionHandler{

    protected static final Logger logger = LogManager.getLogger();

    static ConfigurationProvider provider;
    
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
        provider = new ConfigurationProvider();
    }
    
    @AfterAll
	public static void end() {
		logger.debug("end");
	}

    @BeforeEach
    public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
        consumerAll = new ConsumerTestUnit(provider, "ALL");
        randomProducer = new Producer(provider.getJsap(), "RANDOM", provider.getSecurityManager());
        randomAggregator = new AggregatorTestUnit(provider, "RANDOM", "RANDOM1");
        consumerRandom1 = new ConsumerTestUnit(provider, "RANDOM1");
        genericClient = new GenericClient(provider.getJsap(), provider.getSecurityManager(), this);
    }

    @AfterEach
    public void afterTest() throws IOException {
        consumerAll.close();
        randomProducer.close();
        randomAggregator.close();
        consumerRandom1.close();
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void produceX100() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
            SEPAProtocolException, SEPABindingsException {
        for (int i = 0; i < 100; i++) {
            Response ret = randomProducer.update(provider.TIMEOUT,provider.NRETRY);
            assertFalse(ret.isError(),"Failed on update: "+i);
        }
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 30000)
    public void produceX1000() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
            SEPAProtocolException, SEPABindingsException {
        for (int i = 0; i < 1000; i++) {
            Response ret = randomProducer.update(provider.TIMEOUT,provider.NRETRY);
            assertFalse(ret.isError(),"Failed on update: "+i);
        }
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void aggregationX10() throws InterruptedException, SEPASecurityException, IOException,
            SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
        consumerRandom1.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
        consumerRandom1.waitFirstNotification();

        randomAggregator.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
        randomAggregator.waitFirstNotification();

        for (int i = 0; i < 10; i++) {
            randomProducer.update(provider.TIMEOUT,provider.NRETRY);

            randomAggregator.waitNotification();
            consumerRandom1.waitNotification();
        }
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 10000)
    public void aggregationX100() throws InterruptedException, SEPASecurityException, IOException,
            SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
        consumerRandom1.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
        consumerRandom1.waitFirstNotification();

        randomAggregator.syncSubscribe(provider.TIMEOUT,provider.NRETRY);
        randomAggregator.waitFirstNotification();

        for (int i = 0; i < 100; i++) {
            randomProducer.update(provider.TIMEOUT,provider.NRETRY);

            randomAggregator.waitNotification();
            consumerRandom1.waitNotification();
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
