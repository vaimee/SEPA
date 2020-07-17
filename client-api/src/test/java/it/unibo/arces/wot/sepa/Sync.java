package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.protocols.websocket.WebsocketSubscriptionProtocol;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class Sync implements ISubscriptionHandler, Closeable {
	protected final Logger logger = LogManager.getLogger();

	private static long events = 0;
	private static long subscribes = 0;
	private static long unsubscribes = 0;
	private static long connections = 0;

	private static Object eventsMutex = new Object();
	private static Object subscribesMutex = new Object();
	private static Object unsubscribesMutex = new Object();
	private static Object connectionsMutex = new Object();

	private static ConcurrentHashMap<String, String> spuid;

	private WebsocketSubscriptionProtocol client = null;
	private ConfigurationProvider provider = null;

	public Sync(ConfigurationProvider provider) throws SEPASecurityException, SEPAProtocolException {
		this.provider = provider;
		this.client = provider.getWebsocketClient();
	}
	
	public Sync()  {

	}

	public void reset() {
		logger.trace("reset");
		subscribes = 0;
		events = 0;
		unsubscribes = 0;
		connections = 0;
		spuid = new ConcurrentHashMap<String, String>();
	}

	public void close() {
		if (client != null) {
			for (Entry<String, String> id : spuid.entrySet()) {
				try {
					client.unsubscribe(provider.buildUnsubscribeRequest(id.getKey()));
				} catch (SEPAProtocolException e) {
					logger.error(e.getMessage());
				}
			}
			try {
				client.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		}
	}

	public long getSubscribes() {
		return subscribes;
	}

	public long getEvents() {
		return events;
	}

	public long getUnsubscribes() {
		return unsubscribes;
	}

	public void waitSubscribes(int total) {
		synchronized (subscribesMutex) {
			while (subscribes < total) {
				try {
					logger.trace("waitSubscribes");
					subscribesMutex.wait(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public void onConnection() {
		synchronized (connectionsMutex) {
			logger.trace("onUnsubscribe");
			connections++;
			connectionsMutex.notify();
		}
	}

	public void waitConnections(int total) {
		synchronized (connectionsMutex) {
			while (connections < total) {
				try {
					logger.trace("waitEvents");
					connectionsMutex.wait(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public void waitEvents(int total) {
		synchronized (eventsMutex) {
			while (events < total) {
				try {
					logger.trace("waitEvents");
					eventsMutex.wait(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public void waitUnsubscribes(int total) {
		synchronized (unsubscribesMutex) {
			while (unsubscribes < total) {
				try {
					logger.trace("waitUnsubscribes");
					unsubscribesMutex.wait(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		synchronized (eventsMutex) {
			logger.trace("onSemanticEvent");
			events++;
			eventsMutex.notify();
		}
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		logger.error(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
	}

	@Override
	public void onSubscribe(String id, String alias) {
		synchronized (subscribesMutex) {
			logger.trace("onSubscribe");
			spuid.put(id, alias);
			subscribes++;
			subscribesMutex.notify();
		}
	}

	@Override
	public void onUnsubscribe(String id) {
		synchronized (unsubscribesMutex) {
			logger.trace("onUnsubscribe");
			spuid.remove(id);
			unsubscribes++;
			unsubscribesMutex.notify();
		}
	}

}
