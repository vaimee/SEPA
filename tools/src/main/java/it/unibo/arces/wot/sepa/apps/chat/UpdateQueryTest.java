package it.unibo.arces.wot.sepa.apps.chat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.GenericClient;

public class UpdateQueryTest extends GenericClient  {
	private static final Logger logger = LogManager.getLogger();

	private String SEND;
	private String SET_RECEIVED;
	private String REMOVE;
	private String DELETE_ALL;
	private String REGISTER_USER;

	private String SENT;
	private String RECEIVED;
	private String USERS;

	private int clients;

	public UpdateQueryTest(ISubscriptionHandler handler) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("chat.jsap"), handler);

		SEND = appProfile.update("SEND");
		SET_RECEIVED = appProfile.update("SET_RECEIVED");
		REMOVE = appProfile.update("REMOVE");
		DELETE_ALL = appProfile.update("DELETE_ALL");
		REGISTER_USER = appProfile.update("REGISTER_USER");

		SENT = appProfile.subscribe("SENT");
		RECEIVED = appProfile.subscribe("RECEIVED");
		USERS = appProfile.subscribe("USERS");

		clients = appProfile.getExtendedData().get("clients").getAsInt();
		
		//Logging
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		System.setProperty("logFilename", nowAsISO);
		org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();
	}
	
	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		BasicHandler handler = new BasicHandler();
		UpdateQueryTest test = new UpdateQueryTest(handler);
		
		test.run();
	}

	public void run() {
		deleteAll();

		for (int i = 0; i < clients; i++) {
			registerUser("User"+i);
		}
		
		List<String> users = users();
		
		// 1 - SEND
		for (String receiver : users) send(users.get(0),receiver,"Message");
		
		// 2 - SENT
		List<String> messages = new ArrayList<String>();
		for (String receiver : users) {
			List<String> msg = sent(receiver);
			messages.addAll(msg);
		}
		
		// 3 - SET RECEIVED
		for(String message : messages) setReceived(message);
		
		// 4 - RECEIVED
		messages.clear();
		List<String> msg = received(users.get(0));
		messages.addAll(msg);
		
		// 5 - REMOVE
		for(String message : messages) remove(message);
	}

	public void send(String sender, String receiver, String text) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(sender));
		bindings.addBinding("receiver", new RDFTermURI(receiver));
		bindings.addBinding("text", new RDFTermLiteral(text));

		long start = new Date().toInstant().toEpochMilli();
		update(SEND, bindings);
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SEND "+(stop-start));
	}

	public void setReceived(String message) {
		Bindings bindings = new Bindings();
		bindings.addBinding("message", new RDFTermURI(message));

		long start = new Date().toInstant().toEpochMilli();
		update(SET_RECEIVED, bindings);
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SET_RECEIVED "+(stop-start));
	}

	public void remove(String message) {
		Bindings bindings = new Bindings();
		bindings.addBinding("message", new RDFTermURI(message));
		
		long start = new Date().toInstant().toEpochMilli();
		update(REMOVE, bindings);
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("REMOVE "+(stop-start));
	}

	public void deleteAll() {
		update(DELETE_ALL, null);
	}

	public boolean registerUser(String userName) {
		Bindings bindings = new Bindings();
		bindings.addBinding("userName", new RDFTermLiteral(userName));

		return this.update(REGISTER_USER, bindings).isUpdateResponse();
	}

	public List<String> sent(String receiver) {
		Bindings bindings = new Bindings();
		bindings.addBinding("receiver", new RDFTermURI(receiver));
		
		ArrayList<String> list = new ArrayList<String>();

				
		long start = new Date().toInstant().toEpochMilli();
		Response ret = this.query(SENT, bindings);
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SENT "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			list.add(result.getBindingValue("message"));
		}
		
		return list;
	}

	public List<String> received(String sender) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(sender));

		ArrayList<String> list = new ArrayList<String>();

		long start = new Date().toInstant().toEpochMilli();
		Response ret = this.query(RECEIVED, bindings);
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("RECEIVED "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			list.add(result.getBindingValue("message"));
		}
		
		return list;
	}

	public List<String> users() {
		Response ret = this.query(USERS, null);
		ArrayList<String> list = new ArrayList<String>();
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings bindings : results.getBindingsResults().getBindings()) {
			list.add(bindings.getBindingValue("user"));
		}
		
		return list;
	}
}
