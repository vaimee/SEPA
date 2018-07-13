package it.unibo.arces.wot.sepa.api.protocol.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unibo.arces.wot.sepa.api.MockSubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocol.websocket.SEPAWebsocketClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;

import static org.junit.Assert.*;

public class ITSEPAWebsocketClient {
	private static int N_THREADS = 100;
	private static AtomicLong mutex = new AtomicLong(N_THREADS);

	@BeforeClass
	public static void init() throws SEPAPropertiesException, SEPASecurityException {
		
	}

	@Test(timeout = 10000)
	public void Subscribe() {
		mutex = new AtomicLong(N_THREADS);
		
		for (int i = 0; i < N_THREADS; i++) {
			new Thread() {
				public void run() {
					URI ws = null;
					try {
						ws = new URI("ws://localhost:9000/subscribe");
					} catch (URISyntaxException e) {
						assertFalse(e.getMessage(), true);
					}
					MockSubscriptionHandler handler = new MockSubscriptionHandler();

					SEPAWebsocketClient client = new SEPAWebsocketClient(ws, handler);
					try {
						client.connectBlocking();
					} catch (InterruptedException e) {
						assertFalse(e.getMessage(), true);
					}
					if(client.isOpen()) {
						client.send("{\"subscribe\":{\"sparql\":\"select * where {?x ?y ?z}\",\"default-graph-uri\":\"http://sepatest\"}}");
						client.close();
					}

					synchronized (mutex) {
						mutex.set(mutex.get()-1);
						mutex.notify();
					}
				}
			}.start();
		}

		synchronized (mutex) {
			while (mutex.get() > 0)
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					assertFalse(e.getMessage(), true);
				}
		}
	}
	
	@Test(timeout = 10000)
	public void connectSendAndReceive() {
		mutex = new AtomicLong(N_THREADS);
		
		for (int i = 0; i < N_THREADS; i++) {
			new Thread() {
				public void run() {
					URI ws = null;
					try {
						ws = new URI("ws://localhost:9000/subscribe");
					} catch (URISyntaxException e) {
						assertFalse(e.getMessage(), true);
					}
					MockSubscriptionHandler handler = new MockSubscriptionHandler();

					SEPAWebsocketClient client = new SEPAWebsocketClient(ws, handler);
					try {
						client.connectBlocking();
					} catch (InterruptedException e) {
						assertFalse(e.getMessage(), true);
					}
					if(client.isOpen()) {
						Response ret = client.sendAndReceive("{\"subscribe\":{\"sparql\":\"select * where {?x ?y ?z}\",\"default-graph-uri\":\"http://sepatest\"}}", 2000);
						
						assertFalse(ret.toString(),ret.isError());
						
						client.close();
					}

					synchronized (mutex) {
						mutex.set(mutex.get()-1);
						mutex.notify();
					}
				}
			}.start();
		}

		synchronized (mutex) {
			while (mutex.get() > 0)
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					assertFalse(e.getMessage(), true);
				}
		}
	}

}
