package com.vaimee.sepa;

import java.util.concurrent.ConcurrentHashMap;

import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.api.commons.response.ErrorResponse;
import com.vaimee.sepa.api.commons.response.Notification;
import com.vaimee.sepa.logging.Logging;

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

	public Sync() {

	}

	public synchronized void reset() {
		Logging.getLogger().trace("Sync reset");
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

	public synchronized void waitSubscribes(int total) {
		synchronized (subscribesMutex) {
			while (subscribes < total) {
				try {
					Logging.getLogger().trace("Thread id "+Thread.currentThread().threadId()+ " waitSubscribes");
					subscribesMutex.wait();
					Logging.getLogger().trace("Thread id "+Thread.currentThread().threadId()+ " awaken from waitSubscribes");
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}
	}

	public void onConnection() {
		synchronized (connectionsMutex) {
			Logging.getLogger().trace("onUnsubscribe");
			connections++;
			connectionsMutex.notify();
		}
	}

	public void waitConnections(int total) {
		synchronized (connectionsMutex) {
			while (connections < total) {
				try {
					Logging.getLogger().trace("waitEvents");
					connectionsMutex.wait();
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public synchronized void waitEvents(int total) {
		synchronized (eventsMutex) {
			while (events < total) {
				try {
					Logging.getLogger().trace("Thread id "+Thread.currentThread().threadId()+ " waitEvents");
					eventsMutex.wait();
					Logging.getLogger().trace("Thread id "+Thread.currentThread().threadId()+ " awaken from waitEvents");
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}
	}

	public synchronized  void waitUnsubscribes(int total) {
		synchronized (unsubscribesMutex) {
			while (unsubscribes < total) {
				try {
					Logging.getLogger().trace("waitUnsubscribes");
					unsubscribesMutex.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		}
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		synchronized (eventsMutex) {
			Logging.getLogger().trace("onSemanticEvent");
			events++;
			eventsMutex.notify();
		}
	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		if (errorResponse.getStatusCode() != 1000) Logging.getLogger().error(errorResponse);
		else Logging.getLogger().warn(errorResponse);
	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		Logging.getLogger().error(errorResponse);
	}

	@Override
	public void onSubscribe(String id, String alias) {
		synchronized (subscribesMutex) {
			Logging.getLogger().trace("onSubscribe");
			spuid.put(alias, id);
			subscribes++;
			subscribesMutex.notify();
		}
	}

	@Override
	public void onUnsubscribe(String id) {
		synchronized (unsubscribesMutex) {
			Logging.getLogger().trace("onUnsubscribe");
			spuid.remove(id);
			unsubscribes++;
			unsubscribesMutex.notify();
		}
	}

	public synchronized void onSubscribe() {
		subscribes++;
	}

	public synchronized void onUnsubscribe() {
		unsubscribes++;
	}
	
	public synchronized void onSemanticEvent() {
		events++;
	}

}
