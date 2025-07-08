package com.vaimee.sepa.apps.chat;


import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.vaimee.sepa.api.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.QueryResponse;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.api.commons.sparql.Bindings;
import com.vaimee.sepa.api.commons.sparql.RDFTermLiteral;
import com.vaimee.sepa.api.commons.sparql.RDFTermURI;
import com.vaimee.sepa.api.pattern.GenericClient;

public class UpdateQueryTest {
	private static final Logger logger = LogManager.getLogger();

	private static int clients;

	private static JSAPProvider cfg;
	
	private GenericClient client;
	
	private class Message {
		public String sender;
		public String receiver;
		public String timeSent;
		public String timeReceived;
	};
	
	@BeforeAll
	public static void init() throws SEPASecurityException, SEPAPropertiesException, SEPAProtocolException {
		cfg = new JSAPProvider();

		clients = cfg.getJsap().getExtendedData().get("clients").getAsInt();
	}
	
	//@Test(timeout = 5000)
	@Test
	public void test() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {
		client = new GenericClient(cfg.getJsap(),new BasicHandler());
		
		run();
	}

	public void run() {
		deleteAll();

		for (int i = 0; i < clients; i++) {
			registerUser("User"+i);
		}
		
		List<String> users = queryUsers();
		
		// 1 - SEND
		for (String receiver : users) send(users.get(0),receiver,"Message");
		
		// 2 - SENT
		List<Message> messages = new ArrayList<Message>();
		for (String receiver : users) {
			List<Message> msg = sent(receiver);
			messages.addAll(msg);
		}
		
		// 3 - SET RECEIVED
		for(Message message : messages) setReceived(message);
		
		// 4 - RECEIVED
		messages.clear();
		List<Message> msg = received(users.get(0));
		messages.addAll(msg);
		
		// 5 - REMOVE
		for(Message message : messages) remove(message);
	}

	public void send(String sender, String receiver, String text) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(sender));
		bindings.addBinding("receiver", new RDFTermURI(receiver));
		bindings.addBinding("text", new RDFTermLiteral(text));

		long start = new Date().toInstant().toEpochMilli();
		try {
			client.update("SEND", bindings,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"SEND FAILED "+e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SEND "+(stop-start));
	}

	public void setReceived(Message m) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(m.sender));
		bindings.addBinding("receiver", new RDFTermURI(m.receiver));
		bindings.addBinding("sentTime", new RDFTermLiteral(m.timeSent));
		
		long start = new Date().toInstant().toEpochMilli();
		try {
			client.update("SET_RECEIVED", bindings,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"SET_RECEIVED " +e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SET_RECEIVED "+(stop-start));
	}

	public void remove(Message m) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(m.sender));
		bindings.addBinding("time", new RDFTermLiteral(m.timeReceived));
		
		long start = new Date().toInstant().toEpochMilli();
		try {
			client.update("REMOVE", bindings,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"REMOVE "+e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("REMOVE "+(stop-start));
	}

	public void deleteAll() {
		try {
			client.update("DELETE_ALL", null,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"DELETE_ALL "+e.getMessage());
		}
	}

	public void registerUser(String userName) {
		Bindings bindings = new Bindings();
		bindings.addBinding("userName", new RDFTermLiteral(userName));
		bindings.addBinding("user", new RDFTermURI("http://wot.arces.unibo.it/chat/user/"+userName));
		
		try {
			client.update("REGISTER_USER", bindings,cfg.getTimeout(),cfg.getNRetry()).isUpdateResponse();
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"REGISTER_USER "+e.getMessage());
		}
	}

	public List<Message> sent(String receiver) {
		Bindings bindings = new Bindings();
		bindings.addBinding("receiver", new RDFTermURI(receiver));
		
		ArrayList<Message> list = new ArrayList<Message>();
			
		long start = new Date().toInstant().toEpochMilli();
		Response ret;
		try {
			ret = client.query("SENT", bindings,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"SENT "+e.getMessage());
			return list;
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SENT "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			Message m = new Message();
			m.receiver = receiver;
			m.sender = result.getValue("sender");
			m.timeSent = result.getValue("time");
			list.add(m);
		}
		
		return list;
	}

	public List<Message> received(String sender) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(sender));

		ArrayList<Message> list = new ArrayList<Message>();

		long start = new Date().toInstant().toEpochMilli();
		Response ret;
		try {
			ret = client.query("RECEIVED", bindings,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"RECEIVED "+e.getMessage());
			return list;
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("RECEIVED "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			Message m = new Message();
			m.sender = sender;
			m.timeReceived = result.getValue("time");
		}
		
		return list;
	}

	public List<String> queryUsers() {
		ArrayList<String> list = new ArrayList<String>();
		
		Response ret;
		try {
			ret = client.query("USERS", null,cfg.getTimeout(),cfg.getNRetry());
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException | SEPABindingsException e) {
			assertFalse(true,"USERS "+e.getMessage());
			return list;
		}
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			list.add(bindings.getValue("user"));
		}
		
		return list;
	}
}
