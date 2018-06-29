package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class SEPAChatTest {
	private static final Logger logger = LogManager.getLogger();

	private int N_CLIENTS = 2;
	private int BASE = 0;
	private int MESSAGES = 10;
	private Users users;
	private static List<Thread> clients = new ArrayList<Thread>();
	
	private enum CLIENT_TYPE {
		PING_PONG, BASIC
	};

	private CLIENT_TYPE type = CLIENT_TYPE.BASIC;

	public SEPAChatTest() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		try {
			JSAP app = new JSAP("chat.jsap");
			BASE = app.getExtendedData().get("base").getAsInt();
			N_CLIENTS = app.getExtendedData().get("clients").getAsInt();
			String sType = app.getExtendedData().get("type").getAsString();
			switch(sType.toUpperCase()) {
			case "BASIC":
				type = CLIENT_TYPE.BASIC;
				MESSAGES = app.getExtendedData().get("messages").getAsInt();
				break;
			case "PINGPONG":
				type = CLIENT_TYPE.PING_PONG;
				break;
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		DeleteAll client = new DeleteAll();
		client.clean();
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		
		// Register chat BOTS
		UserRegistration registration = new UserRegistration();
		for (int i = BASE; i < BASE + N_CLIENTS; i++) {
			registration.register("ChatBot" + i);
		}
		try {
			registration.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		users = new Users();
	}

	public boolean start() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		if (!users.joinChat())
			return false;

		for (String user : users.getUsers()) {
			ChatClient client = null;
			switch (type) {
			case BASIC:
				client = new BasicClient(user, users,MESSAGES);
				break;
			case PING_PONG:
				client = new PingPongClient(user, users);
				break;
			default:
				client = new BasicClient(user, users,MESSAGES);
			}

			Thread th = new Thread(client);
			clients.add(th);
			th.start();
		}

		return true;
	}

	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, InterruptedException {

		SEPAChatTest chat = new SEPAChatTest();

		try {
			if (!chat.start())
				System.exit(-1);
		} catch (Exception e) {
			logger.error(e.getMessage());
			System.exit(-1);
		}

		for (Thread th:clients) th.join(60000);
		
		System.exit(0);
	}
}
