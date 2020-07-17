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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static org.junit.Assert.*;


public class StressUsingSPARQLProtocol {
    protected final Logger logger = LogManager.getLogger();

    private static JSAP properties = null;
    private static ConfigurationProvider provider;
    private final static String VALID_ID = "SEPATest";
    private static Sync sync; 
    private static SPARQL11Protocol client;
    private final ArrayList<Subscriber> subscribers = new ArrayList<Subscriber>();
    private final ArrayList<Publisher> publishers = new ArrayList<Publisher>();

    @BeforeClass
    public static void init() throws Exception {
        provider = new ConfigurationProvider();
        properties = provider.getJsap();
        sync = new Sync();
        
        if (properties.isSecure()) {
            // Registration
            Response response = provider.getSecurityManager().register(VALID_ID);
            assertFalse(response.toString(), response.isError());
        }
    }

    @Before
    public void beginTest() throws IOException, SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
            URISyntaxException, InterruptedException {

        sync.reset();

        client = new SPARQL11Protocol(provider.getSecurityManager());

        subscribers.clear();
        publishers.clear();

        Response ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));

        if (ret.isError()) {
            ErrorResponse error = (ErrorResponse) ret;
            if (error.isTokenExpiredError() && properties.isSecure()) provider.getSecurityManager().refreshToken();
            ret = client.update(provider.buildUpdateRequest("DELETE_ALL"));
        }

        logger.debug(ret);

        assertFalse(String.valueOf(ret), ret.isError());
    }

    @After
    public void endTest() throws IOException, InterruptedException {
        client.close();

        for (Subscriber sub : subscribers)
            sub.close();

        for (Publisher pub : publishers)
            pub.close();
        
        sync.close();
    }

    @Test(timeout = 15000)
    public void RequestToken() throws SEPASecurityException, SEPAPropertiesException, InterruptedException {
        ThreadGroup threadGroup = new ThreadGroup("TokenRequestGroup");
        if (properties.isSecure()) {
            for (int n = 0; n < 10; n++) {
                new Thread(threadGroup,null,"TokenThread-"+n) {
                    public void run() {
                        // Registration
                        Response response = null;
                        try {
                            response = provider.getSecurityManager().register(VALID_ID);
                            logger.debug(response);
                        } catch (SEPASecurityException | SEPAPropertiesException e1) {
                            assertFalse(e1.getMessage(),true);
                        }
                        assertFalse("Failed to register a valid ID", response.isError());

                        for (int i = 0; i < 100; i++) {
                            String authorization = null;
                            try {
                                authorization = provider.getSecurityManager().getAuthorizationHeader();
                                if (authorization == null) provider.getSecurityManager().refreshToken();
                                authorization = provider.getSecurityManager().getAuthorizationHeader();
                            } catch (SEPASecurityException | SEPAPropertiesException e1) {
                                assertFalse(e1.getMessage(),true);
                            }
                            assertFalse("Failed to get authorization header", authorization == null);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                return;
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

    @Test(timeout = 10000)
    public void Subscribe3xN()
            throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, InterruptedException {
        int n = 5;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber("ALL", sync));
            subscribers.add(new Subscriber("RANDOM", sync));
            subscribers.add(new Subscriber("RANDOM1", sync));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
                sync.getSubscribes() != subscribers.size());
        assertFalse("Events:" + sync.getEvents() + "(" + subscribers.size() + ")",
                sync.getEvents() != subscribers.size());
    }

    @Test (timeout = 5000)
    public void NotifyNxN() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
            SEPAPropertiesException, SEPASecurityException {

        int n = 5;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber("RANDOM", sync));
            publishers.add(new Publisher("RANDOM", n));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(subscribers.size() + subscribers.size() * publishers.size() * publishers.size());

        assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
                sync.getSubscribes() != subscribers.size());
        assertFalse(
                "Events:" + sync.getEvents() + "(" + subscribers.size()
                        + subscribers.size() * publishers.size() * publishers.size() + ")",
                sync.getEvents() != subscribers.size() + subscribers.size() * publishers.size() * publishers.size());
    }

    @Test (timeout = 200000)
    public void NotifyNx2NWithMalformedUpdates() throws IOException, IllegalArgumentException, SEPAProtocolException,
            InterruptedException, SEPAPropertiesException, SEPASecurityException {

        int n = 10;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber("RANDOM", sync));
            publishers.add(new Publisher("RANDOM", n));
            publishers.add(new Publisher("WRONG", n));
        }

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(subscribers.size() + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2));

        assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
                sync.getSubscribes() != subscribers.size());
        assertFalse(
                "Events:" + sync.getEvents() + "(" + subscribers.size()
                        + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2) + ")",
                sync.getEvents() != subscribers.size()
                        + subscribers.size() * (publishers.size() / 2) * (publishers.size() / 2));
    }

    @Test(timeout = 200000)
    public void UpdateHeavyLoad() throws InterruptedException, SEPAPropertiesException, SEPASecurityException {
        int n = 5;

        for (int i = 0; i < n; i++) {
            publishers.add(new Publisher("RANDOM", n));
            publishers.add(new Publisher("RANDOM1", n));
            publishers.add(new Publisher("VAIMEE", n));
        }

        for (Publisher pub : publishers)
            pub.start();

        // Wait all publishers to complete
        for (Publisher pub : publishers)
            pub.join();
    }

    @Test (timeout = 60000)
    public void Notify3Nx2N() throws IOException, IllegalArgumentException, SEPAProtocolException, InterruptedException,
            SEPAPropertiesException, SEPASecurityException {
        int n = 15;

        for (int i = 0; i < n; i++) {
            subscribers.add(new Subscriber("ALL", sync));
            subscribers.add(new Subscriber("RANDOM", sync));
            subscribers.add(new Subscriber("RANDOM1", sync));

            publishers.add(new Publisher("RANDOM", n));
            publishers.add(new Publisher("RANDOM1", n));
        }

        int events = 4 * n * n * n + subscribers.size();

        for (Subscriber sub : subscribers)
            sub.start();

        sync.waitSubscribes(subscribers.size());
        sync.waitEvents(subscribers.size());

        for (Publisher pub : publishers)
            pub.start();

        sync.waitEvents(events);

        assertFalse("Subscribes:" + sync.getSubscribes() + "(" + subscribers.size() + ")",
                sync.getSubscribes() != subscribers.size());
        assertFalse("Events:" + sync.getEvents() + "(" + events + ")", sync.getEvents() != events);
    }
}
