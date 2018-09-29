package it.unibo.arces.wot.sepa;

public class Sync {
	private static long events = 0;
	private static long subscribes = 0;
	private static long unsubscribes = 0;
	private static String spuid = null;

	public synchronized void reset() {
		subscribes = 0;
		events = 0;
		unsubscribes = 0;
		spuid = null;
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

	public synchronized void resetSpuid() {
		spuid = null;
	}

	public synchronized String getSpuid() {
		return spuid;
	}

	public synchronized void waitSubscribes(int total) {
		while (subscribes < total) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public synchronized void waitEvents(int total) {
		while (events < total) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public synchronized void waitUnsubscribes(int total) {
		while (unsubscribes < total) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public synchronized void event() {
		events++;
		notify();
	}

	public synchronized void unsubscribe() {
		unsubscribes++;
		notify();
	}

	public synchronized void subscribe(String id, String alias) {
		spuid = id;
		subscribes++;
		notify();
	}

}
