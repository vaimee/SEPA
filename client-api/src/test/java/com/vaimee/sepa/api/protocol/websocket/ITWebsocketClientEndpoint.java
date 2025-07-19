package com.vaimee.sepa.api.protocol.websocket;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;

import com.vaimee.sepa.ConfigurationProvider;
import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.api.protocols.websocket.WebsocketClientEndpoint;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.logging.Logging;

public class ITWebsocketClientEndpoint implements ISubscriptionHandler {
	private static ConfigurationProvider provider;
	private static URI url;

	private static Object mutex = new Object();
	private static String spuid = null;
	private static boolean error = false;
	private static boolean results = false;

	@BeforeAll
	public static void init()
			throws SEPAPropertiesException, SEPASecurityException, InterruptedException, SEPAProtocolException {
		provider = new ConfigurationProvider();

		// Connect
		String scheme = "ws://";
		if (provider.getClientSecurityManager() != null)
			scheme = "wss://";
		if (provider.getJsap().getSubscribePort() == -1)
			try {
				url = new URI(scheme + provider.getJsap().getSubscribeHost() + provider.getJsap().getSubscribePath());
			} catch (URISyntaxException e) {
				Logging.getLogger().error(e.getMessage());
				throw new SEPAProtocolException(e);
			}
		else
			try {
				url = new URI(scheme + provider.getJsap().getSubscribeHost() + ":"
						+ provider.getJsap().getSubscribePort() + provider.getJsap().getSubscribePath());
			} catch (URISyntaxException e) {
				Logging.getLogger().error(e.getMessage());
				throw new SEPAProtocolException(e);
			}
	}

	@AfterAll
	public static void end() throws IOException {

	}

	@BeforeEach
	public void before() throws IOException, InterruptedException {
		ITWebsocketClientEndpoint.spuid = null;
		ITWebsocketClientEndpoint.error = false;
		ITWebsocketClientEndpoint.results = false;
		
		Thread.sleep(ConfigurationProvider.SLEEP);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void Connect() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, IOException {
		WebsocketClientEndpoint client = new WebsocketClientEndpoint(provider.getClientSecurityManager(), this);
		client.connect(url);
		client.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void Subscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, IOException,
			InterruptedException {
		WebsocketClientEndpoint client = new WebsocketClientEndpoint(provider.getClientSecurityManager(), this);
		client.connect(url);
		client.send(provider.buildSubscribeRequest("ALL").toString());
		synchronized (mutex) {
			while (ITWebsocketClientEndpoint.spuid == null)
				mutex.wait();
		}
		client.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void SubscribeAndResults() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException,
			IOException, InterruptedException {
		WebsocketClientEndpoint client = new WebsocketClientEndpoint(provider.getClientSecurityManager(), this);
		client.connect(url);

		client.send(provider.buildSubscribeRequest("ALL").toString());
		synchronized (mutex) {
			while (ITWebsocketClientEndpoint.spuid == null)
				mutex.wait();
		}
		synchronized (mutex) {
			while (!ITWebsocketClientEndpoint.results)
				mutex.wait();
		}
		client.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void SubscribeAndUnsubscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException,
			IOException, InterruptedException {
		WebsocketClientEndpoint client = new WebsocketClientEndpoint(provider.getClientSecurityManager(), this);
		client.connect(url);

		client.send(provider.buildSubscribeRequest("ALL").toString());
		synchronized (mutex) {
			while (ITWebsocketClientEndpoint.spuid == null)
				mutex.wait();
		}

		client.send(provider.buildUnsubscribeRequest(ITWebsocketClientEndpoint.spuid).toString());
		synchronized (mutex) {
			while (ITWebsocketClientEndpoint.spuid != null)
				mutex.wait();
		}

		client.close();
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	@Timeout(10)
	public void WrongSubscribe() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException,
			IOException, InterruptedException {
		WebsocketClientEndpoint client = new WebsocketClientEndpoint(provider.getClientSecurityManager(), this);
		client.connect(url);
		client.send(provider.buildSubscribeRequest("WRONG").toString());
		synchronized (mutex) {
			while (!ITWebsocketClientEndpoint.error)
				mutex.wait();
		}
		client.close();
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		synchronized (mutex) {
			ITWebsocketClientEndpoint.results = true;
			mutex.notify();
		}
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		assertFalse(true, errorResponse.toString());
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		synchronized (mutex) {
			ITWebsocketClientEndpoint.error = true;
			mutex.notify();
		}
	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		synchronized (mutex) {
			ITWebsocketClientEndpoint.spuid = spuid;
			mutex.notify();
		}
	}

	@Override
	public void onUnsubscribe(String spuid) {
		synchronized (mutex) {
			ITWebsocketClientEndpoint.spuid = null;
			mutex.notify();
		}
	}
}
