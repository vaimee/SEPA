package it.unibo.arces.wot.sepa.stress;

import it.unibo.arces.wot.sepa.AggregatorTestUnit;
import it.unibo.arces.wot.sepa.ConsumerTestUnit;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.arces.wot.sepa.pattern.Producer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.HashMap;

public class StressUsingPAC  implements ISubscriptionHandler{

    protected final Logger logger = LogManager.getLogger();

    protected static JSAP app = null;
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
	
    @BeforeClass
    public static void init() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
        ConfigurationProvider provider = new ConfigurationProvider();
    	try {   	
            app = new ConfigurationProvider().getJsap();
        } catch (SEPAPropertiesException | SEPASecurityException e) {
            assertFalse("Configuration not found", false);
        }

        if (app.isSecure()) {
        	sm = provider.getSecurityManager();
            Response ret = sm.register("SEPATest");
            ret = sm.refreshToken();
            assertFalse(ret.isError());
        }
    }

    @Before
    public void beginTest() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
        consumerAll = new ConsumerTestUnit(app, "ALL", sm);
        randomProducer = new Producer(app, "RANDOM", sm);
        randomAggregator = new AggregatorTestUnit(app, "RANDOM", "RANDOM1", sm);
        consumerRandom1 = new ConsumerTestUnit(app, "RANDOM1", sm);
        genericClient = new GenericClient(app, sm, this);
    }

    @After
    public void afterTest() throws IOException {
        consumerAll.close();
        randomProducer.close();
        randomAggregator.close();
        consumerRandom1.close();
    }

    @Test(timeout = 40000)
    public void produceX100() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
            SEPAProtocolException, SEPABindingsException {
        for (int i = 0; i < 100; i++) {
            Response ret = randomProducer.update();
            assertFalse("Failed on update: "+i,ret.isError());
        }
    }

    @Test(timeout = 60000)
    public void produceX1000() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
            SEPAProtocolException, SEPABindingsException {
        for (int i = 0; i < 1000; i++) {
            Response ret = randomProducer.update();
            assertFalse("Failed on update: "+i,ret.isError());
        }
    }

    @Test(timeout = 40000)
    public void aggregationX10() throws InterruptedException, SEPASecurityException, IOException,
            SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
        consumerRandom1.syncSubscribe();
        consumerRandom1.waitFirstNotification();

        randomAggregator.syncSubscribe();
        randomAggregator.waitFirstNotification();

        for (int i = 0; i < 10; i++) {
            randomProducer.update();

            randomAggregator.waitNotification();
            consumerRandom1.waitNotification();
        }
    }

    @Test(timeout = 60000)
    public void aggregationX100() throws InterruptedException, SEPASecurityException, IOException,
            SEPAPropertiesException, SEPAProtocolException, SEPABindingsException {
        consumerRandom1.syncSubscribe();
        consumerRandom1.waitFirstNotification();

        randomAggregator.syncSubscribe();
        randomAggregator.waitFirstNotification();

        for (int i = 0; i < 100; i++) {
            randomProducer.update();

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
