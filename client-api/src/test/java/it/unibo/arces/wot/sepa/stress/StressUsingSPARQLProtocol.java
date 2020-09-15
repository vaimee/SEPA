package it.unibo.arces.wot.sepa.stress;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.Publisher;
import it.unibo.arces.wot.sepa.Subscriber;
import it.unibo.arces.wot.sepa.Sync;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class StressUsingSPARQLProtocol {
    protected static final Logger logger = LogManager.getLogger();

    private static JSAP properties = null;
    private static ConfigurationProvider provider;
    private static Sync sync; 
    private static SPARQL11Protocol client;
    private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
    private final ArrayList<Publisher> publishers = new ArrayList<Publisher>();
    
    @BeforeAll
    public static void init() throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException {
        provider = new ConfigurationProvider();
        properties = provider.getJsap();
        sync = new Sync(provider.getSecurityManager());
    }
    
    @AfterAll
	public static void end() {
		logger.debug("end");
	}

    @BeforeEach
    public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
            URISyntaxException, InterruptedException {

        sync.reset();

        client = new SPARQL11Protocol(provider.getSecurityManager());

        subscribers.clear();
        publishers.clear();

        Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));

        if (ret.isError()) {
            ErrorResponse error = (ErrorResponse) ret;
            if (error.isTokenExpiredError() && properties.isSecure()) {
            	provider.getSecurityManager().refreshToken();
            	ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
                assertFalse(ret.isError(),ret.toString());
            }
        }

        logger.debug(ret);

        assertFalse(ret.isError(),String.valueOf(ret));
    }

    @AfterEach
    public void endTest() throws IOException, InterruptedException {
        client.close();

        for (Subscriber sub : subscribers)
            sub.close();

        for (Publisher pub : publishers)
            pub.close();
    }

    //@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void RequestToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
        ThreadGroup threadGroup = new ThreadGroup("TokenRequestGroup");
        if (properties.isSecure()) {
            for (int n = 0; n < 10; n++) {
                new Thread(threadGroup,null,"TokenThread-"+n) {
                    public void run() {
                        for (int i = 0; i < 100; i++) {
                        	try {
								Response ret = provider.getSecurityManager().refreshToken();
								assertFalse(ret.isError(),ret.toString());
	                            try {
	                                Thread.sleep(100);
	                            } catch (InterruptedException e) {
	                                return;
	                            }
							} catch (SEPAPropertiesException | SEPASecurityException e2) {
								assertFalse(true,e2.getMessage());
							}                    
                        }
                    }
                }.start();
            }
            while(threadGroup.activeCount() != 0) {
                Thread.sleep(1000);
            }
        }
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void Subscribe3xN()
            throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
        int n = 5;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber(provider,"ALL", sync));
            subscribers.add(new Subscriber(provider,"RANDOM", sync));
            subscribers.add(new Subscriber(provider,"RANDOM1", sync));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        assertFalse(sync.getSubscribes() != subscribers.size(),"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
        assertFalse(sync.getEvents() != subscribers.size(),"Events:" + sync.getEvents() + "(" + subscribers.size() + ")");
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void NotifyNxN() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
            SEPAPropertiesException, SEPASecurityException {

        int n = 5;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber(provider,"RANDOM", sync));
            publishers.add(new Publisher(provider,"RANDOM", n));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(subscribers.size() + subscribers.size() * publishers.size() * publishers.size());

        assertFalse(sync.getSubscribes() != subscribers.size(),"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
        assertFalse(sync.getEvents() != subscribers.size() + subscribers.size() * publishers.size() * publishers.size(),
                "Events:" + sync.getEvents() + "(" + subscribers.size()
                        + subscribers.size() * publishers.size() * publishers.size() + ")");
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 15000)
    public void NotifyNx2NWithMalformedUpdates() throws IOException, IllegalArgumentException, SEPAProtocolException,
            InterruptedException, SEPAPropertiesException, SEPASecurityException {

        int n = 10;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber(provider,"RANDOM", sync));
            publishers.add(new Publisher(provider,"RANDOM", n));
            publishers.add(new Publisher(provider,"WRONG", n));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(subscribers.size() + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2));

        assertFalse(sync.getSubscribes() != subscribers.size(),"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
        assertFalse(sync.getEvents() != subscribers.size()+ subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2),
                "Events:" + sync.getEvents() + "(" + subscribers.size()
                        + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2) + ")");
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 5000)
    public void UpdateHeavyLoad() throws InterruptedException, SEPAPropertiesException, SEPASecurityException {
        int n = 5;

        for (int i = 0; i < n; i++) {
            publishers.add(new Publisher(provider,"RANDOM", n));
            publishers.add(new Publisher(provider,"RANDOM1", n));
            publishers.add(new Publisher(provider,"VAIMEE", n));
        }

        for (Publisher pub : publishers)
            pub.start();

        // Wait all publishers to complete
        for (Publisher pub : publishers)
            pub.join();
    }

    @RepeatedTest(ConfigurationProvider.REPEATED_TEST)
    //(timeout = 60000)
    public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
            SEPAPropertiesException, SEPASecurityException {
        int n = 15;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber(provider,"ALL", sync));
            subscribers.add(new Subscriber(provider,"RANDOM", sync));
            subscribers.add(new Subscriber(provider,"RANDOM1", sync));

            publishers.add(new Publisher(provider,"RANDOM", n));
            publishers.add(new Publisher(provider,"RANDOM1", n));
        }

        int events = 4 * n * n * n + subscribers.size();

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(events);

        assertFalse(sync.getSubscribes() != subscribers.size(),"Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")");
        assertFalse(sync.getEvents() != events,"Events:" + sync.getEvents() + "(" + events + ")");
    }
}
