package it.unibo.arces.wot.sepa.apps.chat;

import java.io.IOException;
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
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.pattern.JSAP;

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
		super(new JSAP("chat.jsap"));

		SEND = appProfile.getSPARQLUpdate("SEND");
		SET_RECEIVED = appProfile.getSPARQLUpdate("SET_RECEIVED");
		REMOVE = appProfile.getSPARQLUpdate("REMOVE");
		DELETE_ALL = appProfile.getSPARQLUpdate("DELETE_ALL");
		REGISTER_USER = appProfile.getSPARQLUpdate("REGISTER_USER");

		SENT = appProfile.getSPARQLQuery("SENT");
		RECEIVED = appProfile.getSPARQLQuery("RECEIVED");
		USERS = appProfile.getSPARQLQuery("USERS");

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
	
	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException {
		BasicHandler handler = new BasicHandler();
		UpdateQueryTest test = new UpdateQueryTest(handler);
		
		test.run();
		test.close();
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
		try {
			update(SEND, bindings,5000);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SEND "+(stop-start));
	}

	public void setReceived(String message) {
		Bindings bindings = new Bindings();
		bindings.addBinding("message", new RDFTermURI(message));

		long start = new Date().toInstant().toEpochMilli();
		try {
			update(SET_RECEIVED, bindings,5000);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SET_RECEIVED "+(stop-start));
	}

	public void remove(String message) {
		Bindings bindings = new Bindings();
		bindings.addBinding("message", new RDFTermURI(message));
		
		long start = new Date().toInstant().toEpochMilli();
		try {
			update(REMOVE, bindings,5000);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("REMOVE "+(stop-start));
	}

	public void deleteAll() {
		try {
			update(DELETE_ALL, null,5000);
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
	}

	public boolean registerUser(String userName) {
		Bindings bindings = new Bindings();
		bindings.addBinding("userName", new RDFTermLiteral(userName));

		try {
			return this.update(REGISTER_USER, bindings,5000).isUpdateResponse();
		} catch (SEPAProtocolException | SEPASecurityException | IOException | SEPAPropertiesException e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	public List<String> sent(String receiver) {
		Bindings bindings = new Bindings();
		bindings.addBinding("receiver", new RDFTermURI(receiver));
		
		ArrayList<String> list = new ArrayList<String>();
			
		long start = new Date().toInstant().toEpochMilli();
		Response ret;
		try {
			ret = this.query(SENT, bindings,5000);
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException e) {
			logger.error(e.getMessage());
			return list;
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("SENT "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			list.add(result.getValue("message"));
		}
		
		return list;
	}

	public List<String> received(String sender) {
		Bindings bindings = new Bindings();
		bindings.addBinding("sender", new RDFTermURI(sender));

		ArrayList<String> list = new ArrayList<String>();

		long start = new Date().toInstant().toEpochMilli();
		Response ret;
		try {
			ret = this.query(RECEIVED, bindings,5000);
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException e) {
			logger.error(e.getMessage());
			return list;
		}
		long stop = new Date().toInstant().toEpochMilli();
		
		logger.info("RECEIVED "+(stop-start));
		
		if (ret.isError()) return list;
		
		QueryResponse results = (QueryResponse) ret;
		for (Bindings result : results.getBindingsResults().getBindings()) {
			list.add(result.getValue("message"));
		}
		
		return list;
	}

	public List<String> users() {
		ArrayList<String> list = new ArrayList<String>();
		
		Response ret;
		try {
			ret = this.query(USERS, null,5000);
		} catch (SEPAProtocolException | SEPASecurityException  | SEPAPropertiesException e) {
			logger.error(e.getMessage());
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
