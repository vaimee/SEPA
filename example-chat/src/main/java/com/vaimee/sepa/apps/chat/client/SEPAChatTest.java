package com.vaimee.sepa.apps.chat.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.vaimee.sepa.logging.Logging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaimee.sepa.apps.chat.ChatClient;
import com.vaimee.sepa.apps.chat.ChatMonitor;
import com.vaimee.sepa.apps.chat.DeleteAll;
import com.vaimee.sepa.apps.chat.JSAPProvider;
import com.vaimee.sepa.apps.chat.UserRegistration;
import com.vaimee.sepa.apps.chat.Users;
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

	public static void main(String[] args) throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, IOException, InterruptedException, SEPABindingsException {
		Logging.init();
		init();
		basicChatTest();
	}
	
	private static void init() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException, IOException {
		cfg = new JSAPProvider();

		BASE = cfg.getJsap().getExtendedData().get("base").getAsInt();
		N_CLIENTS = cfg.getJsap().getExtendedData().get("clients").getAsInt();
		MESSAGES = cfg.getJsap().getExtendedData().get("messages").getAsInt();

		deleteAllClients();
		registerClients();

		users = new Users();
	}

	private static void basicChatTest() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException,
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
		
		System.exit(0);
	}

	private static void deleteAllClients()
			throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException, IOException {
		DeleteAll client = new DeleteAll();
		client.clean();

		client.close();
	}

	private static void registerClients() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException, IOException {
		// Register chat BOTS
		UserRegistration registration = new UserRegistration();
		for (int i = BASE; i < BASE + N_CLIENTS; i++) {
			logger.info("Register client: "+"ChatBot" + i);
			registration.register("ChatBot" + i);
		}

		registration.close();
	}
}
