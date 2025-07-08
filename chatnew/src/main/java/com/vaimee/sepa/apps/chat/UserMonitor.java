package com.vaimee.sepa.apps.chat;

import java.util.HashMap;

public class UserMonitor {
	private final String user;
	private final int messages;

	private final HashMap<String, Integer> sent = new HashMap<>();
	private final HashMap<String, Integer> received = new HashMap<>();
	private final HashMap<String, Integer> removed = new HashMap<>();
	
	public UserMonitor(String user, int messages) {
		this.user = user;
		this.messages = messages;
	}

	public String printDetails() {
		return sent.toString() + "\r\n" + received.toString() + "\r\n" + removed.toString();
	}
	
	public String toString() {
		return user + " => " + messages + "|" + getSent() + "|" + getReceived() + "|" + getRemoved();
	}

	public boolean allDone() {
		if (getSent() != messages) return false;
		if (getReceived() != messages) return false;
		if (getRemoved() != messages) return false;
		
		return true;
	}

	public int getSent() {
		int n=0;
		for (String user : sent.keySet()) {
			n += sent.get(user);
		}
		return n;
	}

	public void setSent(String to) {
		if (sent.containsKey(to)) sent.put(to, this.sent.get(to)+1);
		else sent.put(to, 1);
	}

	public int getReceived() {
		int n=0;
		for (String user : received.keySet()) {
			n += received.get(user);
		}
		return n;
	}

	public void setReceived(String from) {
		if (received.containsKey(from)) received.put(from,  received.get(from) + 1);
		else received.put(from, 1);
	}

	public int getRemoved() {
		int n=0;
		for (String user : removed.keySet()) {
			n += removed.get(user);
		}
		return n;
	}

	public void setRemoved(String to) {
		if (removed.containsKey(to)) removed.put(to,removed.get(to) + 1);
		else removed.put(to,1);
	}
}
