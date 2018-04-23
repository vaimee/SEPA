package it.unibo.arces.wot.sepa.apps.chat;

import java.util.Date;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timings {
	private static final Logger logger = LogManager.getLogger();

	class Timing {
		public long startSend;
		public long stopSend;

		public long setReceivedStart;
		public long setReceivedStop;

		public long removeStart;
		public long removeStop;

		public long sent;
		public long received;
	}

	HashMap<String, Timing> updates = new HashMap<String, Timing>();
	HashMap<String, Timing> messages = new HashMap<String, Timing>();

	public synchronized void startSend(String from, String to, String message) {
		Timing t = new Timing();
		t.startSend = new Date().getTime();
		synchronized (updates) {
			updates.put(from + to + message, t);
		}
	}

	public synchronized void stopSend(String from, String to, String message) {
		if (updates.get(from + to + message) == null)
			return;
		synchronized (updates) {
			updates.get(from + to + message).stopSend = new Date().getTime();
		}
	}

	public synchronized void sent(String messageURI, String from, String to, String message) {
		if (updates.get(from + to + message) == null)
			return;

		synchronized (updates) {
			updates.get(from + to + message).sent = new Date().getTime();
		}
		synchronized (messages) {
			messages.put(messageURI, updates.get(from + to + message));
		}
	}

	public synchronized void setReceivedStart(String messageURI) {
		if (messages.get(messageURI) == null)
			return;
		synchronized (messages) {
			messages.get(messageURI).setReceivedStart = new Date().getTime();
		}
	}

	public synchronized void setReceivedStop(String messageURI) {
		if (messages.get(messageURI) == null)
			return;
		synchronized (messages) {
			messages.get(messageURI).setReceivedStop = new Date().getTime();
		}
	}

	public synchronized void received(String messageURI) {
		if (messages.get(messageURI) == null)
			return;
		synchronized (messages) {
			messages.get(messageURI).received = new Date().getTime();
		}
	}

	public synchronized void removeStart(String messageURI) {
		if (messages.get(messageURI) == null)
			return;
		synchronized (messages) {
			messages.get(messageURI).removeStart = new Date().getTime();
		}
	}

	public synchronized void removeStop(String messageURI) {
		if (messages.get(messageURI) == null)
			return;
		synchronized (messages) {
			messages.get(messageURI).removeStop = new Date().getTime();
		}
	}

	public synchronized void logToFile() {
		synchronized (messages) {
			logger.log(Level.getLevel("timing"),"Send-start Send-stop Sent Set-received-start Set-received-stop Received Remove-start Remove-stop");}
			for (Timing t : messages.values()) {
				String message = String.format("%d %d %d %d %d %d %d %d %d",t.startSend, t.stopSend,t.sent,t.setReceivedStart,t.setReceivedStop,t.received,t.removeStart,t.removeStop);
				logger.log(Level.getLevel("timing"),message);
			}
	}
}
