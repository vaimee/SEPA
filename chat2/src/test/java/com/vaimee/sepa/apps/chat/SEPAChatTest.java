package com.vaimee.sepa.apps.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vaimee.sepa.apps.chat.client.BasicClient;
import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;

public class SEPAChatTest {
	private static final Logger logger = LogManager.getLogger();
	
	private static int N_CLIENTS = 2;
	private static int BASE = 0;
	private static int MESSAGES = 1;

	private static Users users;
	private static List<ChatClient> clients = new ArrayList<ChatClient>();

	private static ChatMonitor monitor;

	private static JSAPProvider cfg;

	@BeforeAll
	public static void init() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		cfg = new JSAPProvider();

		BASE = cfg.getJsap().getExtendedData().get("base").getAsInt();
		N_CLIENTS = cfg.getJsap().getExtendedData().get("clients").getAsInt();
		MESSAGES = cfg.getJsap().getExtendedData().get("messages").getAsInt();

		deleteAllClients();
		registerClients();

		users = new Users();
	}

	@Test // (timeout = 5000)
	public void basicChatTest() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
			InterruptedException, IOException, SEPABindingsException {

		users.joinChat();
		
		monitor = new ChatMonitor(users.getUsers(), MESSAGES);
		monitor.start();
		
		for (String user : users.getUsers()) {
			ChatClient client = new BasicClient(user, users, MESSAGES,monitor);
			clients.add(client);
		}
		
		for (ChatClient client : clients) {
			Thread th = new Thread(client);
			th.start();
		}
		
		monitor.join();
	}

	private static void deleteAllClients()
			throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		DeleteAll client = new DeleteAll();
		client.clean();
		try {
			client.close();
		} catch (IOException e) {
			assertFalse(true,"deleteAllClients");
		}
	}

	private static void registerClients() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		// Register chat BOTS
		UserRegistration registration = new UserRegistration();
		for (int i = BASE; i < BASE + N_CLIENTS; i++) {
			logger.info("Register client: "+"ChatBot" + i);
			registration.register("ChatBot" + i);
		}
		try {
			registration.close();
		} catch (IOException e) {
			assertFalse(true,"registerClients");
		}
	}
}
