package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicLong;

public class Sync {
	private static final AtomicLong events = new AtomicLong(0);
	private static final AtomicLong subscribes = new AtomicLong(0);
	private static final AtomicLong unsubscribes = new AtomicLong(0);
	private String spuid = null;
	
	public void reset() {
		subscribes.set(0);
		events.set(0);
		unsubscribes.set(0);
		spuid = null;
	}

	public long getSubscribes() {
		return subscribes.get();
	}

	public long getEvents() {
		return events.get();
	}
	
	public long getUnsubscribes() {
		return unsubscribes.get();
	}
	
	public void resetSpuid() {
		spuid = null;
	}

	public String getSpuid() {
		return spuid;
	}
	
	public void waitSubscribes(int total) {
		while (subscribes.get() < total) {
			synchronized (subscribes) {
				try {
					subscribes.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}
	}

	public void waitEvents(int total) {
		while (events.get() < total) {
			synchronized (events) {
				try {
					events.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}
	}

	public void waitUnsubscribes(int total) {
		while (unsubscribes.get() < total) {
			synchronized (unsubscribes) {
				try {
					unsubscribes.wait(1000);
				} catch (InterruptedException e) {

				}
			}
		}
		
	}

	public synchronized void event() {
		synchronized (events) {
			events.set(events.get() + 1);
			events.notify();
		}
	}
	
	public synchronized void unsubscribe() {
		synchronized (unsubscribes) {
			unsubscribes.set(unsubscribes.get() + 1);
			unsubscribes.notify();
		}
	}
	
	public synchronized void subscribe(String spuid,String alias) {	
		synchronized (subscribes) {
			this.spuid = spuid;
			subscribes.set(subscribes.get() + 1);
			subscribes.notify();
		}
	}
	
}
