package com.vaimee.sepa.stress;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import com.vaimee.sepa.ConfigurationProvider;
import com.vaimee.sepa.api.ISubscriptionHandler;
import com.vaimee.sepa.commons.exceptions.SEPABindingsException;
import com.vaimee.sepa.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.commons.exceptions.SEPAProtocolException;
import com.vaimee.sepa.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.commons.response.ErrorResponse;
import com.vaimee.sepa.commons.response.Notification;
import com.vaimee.sepa.commons.response.QueryResponse;
import com.vaimee.sepa.commons.response.Response;
import com.vaimee.sepa.commons.sparql.Bindings;
import com.vaimee.sepa.pattern.GenericClient;
import com.vaimee.sepa.pattern.Producer;

public class My2SecEvents implements ISubscriptionHandler {
	static ConfigurationProvider provider;

	protected static GenericClient genericClient;
	protected static Producer deleteAll;

	private String prefixes = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX sepa:<https://github.com/arces-wot/SEPA>";
	private String ADD_TRAINING_EVENT = prefixes + "INSERT {GRAPH <http://sepatest> { ?b rdf:type sepa:TrainingEvent ; sepa:hasMember <http://user/graph>; rdf:type <http://event/type>; sepa:nameApp 'AppName'; sepa:titleFile 'Title'; sepa:inXSDDateTimeStamp 'dateTime'; sepa:hasActivityType <http://activity/type>; sepa:taskTitle 'TaskTitle'; sepa:hasTimeInterval _:d . _:d rdf:type sepa:Duration; sepa:unitType sepa:unitSecond ; sepa:numericDuration 10 }}  WHERE {BIND(UUID() AS ?b )}";
	private String QUERY_TRAINING_EVENT = prefixes+ "SELECT * WHERE {GRAPH <http://sepatest> { ?b rdf:type sepa:TrainingEvent}}";
	private String DELETE_TRAINING_EVENT = prefixes + "DELETE {GRAPH <http://sepatest> {?event ?p ?o ; sepa:hasTimeInterval ?d .?d ?p1 ?o1 } } WHERE{GRAPH <http://sepatest> {?event ?p ?o ; sepa:hasTimeInterval ?d . ?d ?p1 ?o1 } VALUES ?event {%values%}}";
	private String DELETE_ALL_TRAINING_EVENT = prefixes + "DELETE {GRAPH <http://sepatest> {%values%} } WHERE{GRAPH <http://sepatest> {%values%}}";
	
	@BeforeAll
	public static void init() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		provider = new ConfigurationProvider();
	}

	@AfterAll
	public static void end() {
	}

	@BeforeEach
	public void beginTest()
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		genericClient = new GenericClient(provider.getJsap(), this);
		deleteAll = new Producer(provider.getJsap(), "DELETE_ALL");
		deleteAll.update();
	}

	@AfterEach
	public void afterTest() throws IOException, InterruptedException {
		deleteAll.close();
		genericClient.close();
		
		Thread.sleep(ConfigurationProvider.SLEEP);
	}

	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	// (timeout = 5000)
	public void deleteEventsWithValues() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		QueryResponse res;
		Response ret;
		
		for (int i = 0; i < 100; i++) {
			ret = genericClient.update(null, ADD_TRAINING_EVENT, null);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}

		ret = genericClient.query(null, QUERY_TRAINING_EVENT, null);
		assertFalse(ret.isError(), "Failed on query" + ret);

		res = (QueryResponse) ret;
		assertFalse(res.getBindingsResults().getBindings().size() != 100,
				"Results size is wrong" + res.getBindingsResults().getBindings().size());

		String values = "";
		for (Bindings b : res.getBindingsResults().getBindings()) {
			values += "<" + b.getValue("b") + "> ";
		}
		
		ret = genericClient.update(null, DELETE_TRAINING_EVENT.replace("%values%", values), null);
		assertFalse(ret.isError(), "Failed on update: " + ret);
		
		ret = genericClient.query(null, QUERY_TRAINING_EVENT, null);
		assertFalse(ret.isError(), "Failed on query: " + ret);
		res = (QueryResponse) ret;
		assertFalse(res.getBindingsResults().getBindings().size() != 0,
				"Results size is wrong" + res.getBindingsResults().getBindings().size());
	}
	
	@RepeatedTest(ConfigurationProvider.REPEATED_TEST)
	// (timeout = 5000)
	public void deleteAllEvents() throws InterruptedException, SEPASecurityException, IOException, SEPAPropertiesException,
			SEPAProtocolException, SEPABindingsException {
		QueryResponse res;
		Response ret;
		
		for (int i = 0; i < 100; i++) {
			ret = genericClient.update(null, ADD_TRAINING_EVENT, null);
			assertFalse(ret.isError(), "Failed on update: " + i + " " + ret);
		}

		ret = genericClient.query(null, QUERY_TRAINING_EVENT, null);
		assertFalse(ret.isError(), "Failed on query" + ret);

		res = (QueryResponse) ret;
		assertFalse(res.getBindingsResults().getBindings().size() != 100,
				"Results size is wrong " + res.getBindingsResults().getBindings().size() + " on 100");

		String pattern = "?event ?p ?o ; sepa:hasTimeInterval ?d . ?d ?dp ?do";
		String values = "";
		int n = 0;
		for (Bindings b : res.getBindingsResults().getBindings()) {
			String temp = new String(pattern);
			temp.replace("?event", "<" + b.getValue("b") + ">");
			temp.replace("?p", "?p"+n);
			temp.replace("?o", "?o"+n);
			temp.replace("?d", "?d"+n);
			temp.replace("?dp", "?d"+n+1);
			temp.replace("?do", "?d"+n+1);
			values += temp + " . ";
			n +=2;
		}
		
		ret = genericClient.update(null, DELETE_ALL_TRAINING_EVENT.replace("%values%", values), null);
		assertFalse(ret.isError(), "Failed on update: " + ret);
		
		ret = genericClient.query(null, QUERY_TRAINING_EVENT, null);
		assertFalse(ret.isError(), "Failed on query: " + ret);
		res = (QueryResponse) ret;
		assertFalse(res.getBindingsResults().getBindings().size() != 0,
				"Results size is wrong" + res.getBindingsResults().getBindings().size());
	}

	@Override
	public void onSemanticEvent(Notification notify) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBrokenConnection(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribe(String spuid) {
		// TODO Auto-generated method stub

	}
}
