package it.unibo.arces.wot.sepa;


import java.util.concurrent.ConcurrentHashMap;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.logging.Logging;

public class Sync implements ISubscriptionHandler {
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
		Logging.logger.trace("ISubscriptionHandler reset");
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
					Logging.logger.trace("waitSubscribes");
					subscribesMutex.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public void onConnection() {
		synchronized (connectionsMutex) {
			Logging.logger.trace("onUnsubscribe");
			connections++;
			connectionsMutex.notify();
		}
	}

	public void waitConnections(int total) {
		synchronized (connectionsMutex) {
			while (connections < total) {
				try {
					Logging.logger.trace("waitEvents");
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
					Logging.logger.trace("waitEvents");
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
					Logging.logger.trace("waitUnsubscribes");
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
			Logging.logger.trace("onSemanticEvent");
			events++;
			eventsMutex.notify();
		}
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		if (errorResponse.getStatusCode() != 1000) Logging.logger.error(errorResponse);
		else Logging.logger.warn(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.logger.error(errorResponse);
//		if (errorResponse.isTokenExpiredError())
//			try {
//				sm.refreshToken();
//			} catch (SEPAPropertiesException | SEPASecurityException e) {
//				Logging.logger.error("Failed to refresh token. "+e.getMessage());
//			}
	}

	@Override
	public void onSubscribe(String id, String alias) {
		synchronized (subscribesMutex) {
			Logging.logger.trace("onSubscribe");
			spuid.put(alias, id);
			subscribes++;
			subscribesMutex.notify();
		}
	}

	@Override
	public void onUnsubscribe(String id) {
		synchronized (unsubscribesMutex) {
			Logging.logger.trace("onUnsubscribe");
			spuid.remove(id);
			unsubscribes++;
			unsubscribesMutex.notify();
		}
	}

}
