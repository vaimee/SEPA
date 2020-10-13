package it.unibo.arces.wot.sepa;


import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;

public class Sync implements ISubscriptionHandler {
	protected final Logger logger = LogManager.getLogger();

	private static long events = 0;
	private static long subscribes = 0;
	private static long unsubscribes = 0;
	private static long connections = 0;

	private static Object eventsMutex = new Object();
	private static Object subscribesMutex = new Object();
	private static Object unsubscribesMutex = new Object();
	private static Object connectionsMutex = new Object();

	private static ConcurrentHashMap<String, String> spuid = new ConcurrentHashMap<String, String>();

//	private ClientSecurityManager sm;

	public Sync() throws SEPASecurityException, SEPAProtocolException {
//		this.sm = sm;
	}

	public synchronized void reset() {
		logger.trace("ISubscriptionHandler reset");
		subscribes = 0;
		events = 0;
		unsubscribes = 0;
		connections = 0;
		spuid.clear();
	}
	
	public synchronized String getSpuid(String alias) {
		return spuid.get(alias);
	}

	public synchronized long getSubscribes() {
		return subscribes;
	}

	public synchronized long getEvents() {
		return events;
	}

	public synchronized long getUnsubscribes() {
		return unsubscribes;
	}

	public void waitSubscribes(int total) {
		synchronized (subscribesMutex) {
			while (subscribes < total) {
				try {
					logger.trace("waitSubscribes");
					subscribesMutex.wait();
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
					connectionsMutex.wait();
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
					eventsMutex.wait();
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
					unsubscribesMutex.wait();
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
		if (errorResponse.getStatusCode() != 1000) logger.error(errorResponse);
		else logger.warn(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		logger.error(errorResponse);
//		if (errorResponse.isTokenExpiredError())
//			try {
//				sm.refreshToken();
//			} catch (SEPAPropertiesException | SEPASecurityException e) {
//				logger.error("Failed to refresh token. "+e.getMessage());
//			}
	}

	@Override
	public void onSubscribe(String id, String alias) {
		synchronized (subscribesMutex) {
			logger.trace("onSubscribe");
			spuid.put(alias, id);
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
