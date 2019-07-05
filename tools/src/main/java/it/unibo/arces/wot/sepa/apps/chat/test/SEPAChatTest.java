package it.unibo.arces.wot.sepa.apps.chat.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unibo.arces.wot.sepa.apps.chat.DeleteAll;
import it.unibo.arces.wot.sepa.apps.chat.UserRegistration;
import it.unibo.arces.wot.sepa.apps.chat.Users;
import it.unibo.arces.wot.sepa.apps.chat.client.BasicClient;
import it.unibo.arces.wot.sepa.apps.chat.client.ChatClient;
import it.unibo.arces.wot.sepa.apps.chat.client.PingPongClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class SEPAChatTest {

	private int N_CLIENTS = 2;
	private int BASE = 0;
	private int MESSAGES = 10;
	private Users users;
	private List<Thread> clients = new ArrayList<Thread>();

	private final SEPASecurityManager sm;
	private final JSAP app;
	
	private enum CLIENT_TYPE {
		PING_PONG, BASIC
	};

	private static CLIENT_TYPE type = CLIENT_TYPE.BASIC;

	public SEPAChatTest(String jsapFile) throws SEPAPropertiesException, SEPASecurityException, SEPAProtocolException, IOException {
		app = new JSAP(jsapFile);

		BASE = app.getExtendedData().get("base").getAsInt();
		N_CLIENTS = app.getExtendedData().get("clients").getAsInt();
		String sType = app.getExtendedData().get("type").getAsString();
		switch (sType.toUpperCase()) {
		case "BASIC":
			type = CLIENT_TYPE.BASIC;
			MESSAGES = app.getExtendedData().get("messages").getAsInt();
			break;
		case "PINGPONG":
			type = CLIENT_TYPE.PING_PONG;
			break;
		}

		if (app.isSecure()) {
			sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017", app.getAuthenticationProperties());
			sm.register("SEPATest");
		}
		else sm = null;

		deleteAllClients(sm);
		registerClients(sm);

		users = new Users(app,sm);
	}

	public static void main(String[] args) throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			InterruptedException, IOException, SEPABindingsException {
		SEPAChatTest test = new SEPAChatTest(args[0]);
		test.start();
	}

	public void start() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			InterruptedException, IOException, SEPABindingsException {
		
		users.joinChat();
		
		for (String user : users.getUsers()) {
			ChatClient client = null;
			switch (type) {
			case BASIC:
				client = new BasicClient(app,user, users, MESSAGES, sm);
				break;
			case PING_PONG:
				client = new PingPongClient(app,user, users, sm);
				break;
			default:
				client = new BasicClient(app,user, users, MESSAGES, sm);
			}

			Thread th = new Thread(client);
			clients.add(th);
			th.start();
		}

		for (Thread th : clients)
			th.join(60000);

	}

	private void deleteAllClients(SEPASecurityManager sm)
			throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		DeleteAll client = new DeleteAll(app,sm);
		client.clean();
		try {
			client.close();
		} catch (IOException e) {
		}
	}

	private void registerClients(SEPASecurityManager sm)
			throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		// Register chat BOTS
		UserRegistration registration = new UserRegistration(app,sm);
		for (int i = BASE; i < BASE + N_CLIENTS; i++) {
			registration.register("ChatBot" + i);
		}
		try {
			registration.close();
		} catch (IOException e) {
		}
	}
}
