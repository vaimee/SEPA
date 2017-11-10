package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
import java.util.HashMap;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class ExtremeChat extends BasicChatClient implements Runnable {
	public static int N_CLIENTS = 5;
	public static int BASE = 0;
	public static long SLEEP = 1000;
	private int index;

	private HashMap<String, Integer> sent = new HashMap<String, Integer>();
	private HashMap<String, Integer> read = new HashMap<String, Integer>();
	private HashMap<String, Integer> received = new HashMap<String, Integer>();

	private int messageIndex = 0;
	
	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {
		ApplicationProfile app = new ApplicationProfile("chat.jsap");

		if (args.length != 2) {
			System.out.println("Running with default parameters (you can run with two arguments: <BASE> <N_CLIENTS>");
		}
		else
		{
			BASE = Integer.parseInt(args[0]);	
			N_CLIENTS = Integer.parseInt(args[1]);
		}
		
		for (int i = BASE; i < BASE+N_CLIENTS; i++) {
			new Thread(new ExtremeChat(i, app)).start();
		}
		System.out.println("Press a key to exit...");
		System.in.read();
		System.exit(0);
	}

	public ExtremeChat(int index, ApplicationProfile app)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super("ChatBot" + index, app);
		this.index = index;
	}

	public void run() {
		System.out.println("Joining the chat...");
		while (!joinChat()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		System.out.println(getNickname()+ " joined!");
		for (int i = BASE; i < BASE+N_CLIENTS; i++) {
			if (i != index) {
				String to = "ChatBot" + i;
				System.out.println(
						"[" + getNickname() + "]-->[" + to + "]  Message #"+messageIndex);
				sendMessage(to, "Message #"+messageIndex++);
			}
		}

		synchronized (this) {
			try {
				this.wait();
			} catch (InterruptedException e) {

			}
		}
	}

	@Override
	public void onMessageReceived(Message message) {
		if (received.containsKey(message.getFrom())) {
			received.put(message.getFrom(), received.get(message.getFrom()) + 1);
		} else
			received.put(message.getFrom(), 1);
		System.out.println(
				"[" + getNickname() + "]<--[" + message.getFrom() + "] " + message.getText() + " RECEIVED " + received);

		sendMessage(message.getFrom(), "Message #"+messageIndex++);
	}

	@Override
	public void onMessageSent(Message message) {
		if (sent.containsKey(message.getTo())) {
			sent.put(message.getTo(), sent.get(message.getTo()) + 1);
		} else
			sent.put(message.getTo(), 1);
		System.out
				.println("[" + getNickname() + "]-->[" + message.getTo() + "] " + message.getText() + " SENT* " + sent);
	}

	@Override
	public void onMessageRead(Message message) {
		if (read.containsKey(message.getTo())) {
			read.put(message.getTo(), read.get(message.getTo()) + 1);
		} else
			read.put(message.getTo(), 1);
		System.out.println(
				"[" + getNickname() + "]-->[" + message.getTo() + "] " + message.getText() + " READ** " + read);
	}

	@Override
	public void onBrokenConnection() {
		System.out.println(getNickname() + " connection is down!");
		
		while (!joinChat()) {
			try {
				System.out.println("Joining the chat...");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		System.out.println("Chat joined!");
	}
}
