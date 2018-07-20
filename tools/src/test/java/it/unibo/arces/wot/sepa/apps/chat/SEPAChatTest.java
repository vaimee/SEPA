package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.BeforeClass;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.JSAP;

import org.junit.Test;

import static org.junit.Assert.*;

public class SEPAChatTest {
	private static final Logger logger = LogManager.getLogger();

	private static int N_CLIENTS = 2;
	private static int BASE = 0;
	private static int MESSAGES = 10;
	private static Users users;
	private static List<Thread> clients = new ArrayList<Thread>();

	private static enum CLIENT_TYPE {
		PING_PONG, BASIC
	};

	private static CLIENT_TYPE type = CLIENT_TYPE.BASIC;

	@BeforeClass
	public static void init() {
		try {
			JSAP app = ConfigurationProvider.GetTestEnvConfiguration();

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
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

	@Test
	public void chatTest() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException, InterruptedException, IOException {
		deleteAllClients();
		registerClients();
		
		users = new Users();
		users.joinChat();
		
		for (String user : users.getUsers()) {
			ChatClient client = null;
			switch (type) {
			case BASIC:
				client = new BasicClient(user, users, MESSAGES);
				break;
			case PING_PONG:
				client = new PingPongClient(user, users);
				break;
			default:
				client = new BasicClient(user, users, MESSAGES);
			}

			Thread th = new Thread(client);
			clients.add(th);
			th.start();
		}
		
		for (Thread th : clients)
			th.join(60000);
		
		assertTrue("Chat tested",true);
	}

	@Test
	public void deleteAllClients() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		DeleteAll client = new DeleteAll();
		client.clean();
		try {
			client.close();
		} catch (IOException e) {
			assertFalse(e.getMessage(), true);
		}

		assertTrue("Delete all clients SUCCESS", true);
	}

	@Test
	public void registerClients() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		// Register chat BOTS
		UserRegistration registration = new UserRegistration();
		for (int i = BASE; i < BASE + N_CLIENTS; i++) {
			registration.register("ChatBot" + i);
		}
		try {
			registration.close();
		} catch (IOException e) {
			assertFalse(e.getMessage(), true);
		}
		
		assertTrue("Clients registration SUCCESS", true);
	}
}
