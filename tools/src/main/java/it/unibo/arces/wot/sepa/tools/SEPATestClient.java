/* This program can be used and extended to test a SEPA implementation and API

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package it.unibo.arces.wot.sepa.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;

public class SEPATestClient {
	protected Results results;
	protected String spuid = null;
	protected boolean notificationReceived = false;
	protected Object sync = new Object();

	protected long notificationMaxDelay = 2000;
	protected JsonArray sequence;

	private ApplicationProfile appProfile;
	private SubscriptionHandler handler =  new SubscriptionHandler();
	private GenericClient client;
	
	class SubscriptionHandler implements ISubscriptionHandler {
		@Override
		public void onSemanticEvent(Notification notify) {
			synchronized (sync) {
				System.out.println(notify.toString());
				notificationReceived = true;
				sync.notify();
			}
		}

		@Override
		public void onBrokenSocket() {
			System.out.println("Broken socket!");
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			synchronized (sync) {
				System.out.println(errorResponse.toString());
				sync.notify();
			}
		}	
	}
	
	public SEPATestClient(ApplicationProfile appProfile) throws SEPAProtocolException, SEPASecurityException {
		client = new GenericClient(appProfile,handler);

		sequence = appProfile.getExtendedData().getAsJsonObject().get("sequence").getAsJsonArray();
		notificationMaxDelay = appProfile.getExtendedData().getAsJsonObject().get("notificationMaxDelay").getAsLong();

		this.appProfile = appProfile;
	}

	class Results {
		private long failed;
		private ArrayList<Result> results = new ArrayList<Result>();

		public void addResult(String title, boolean success) {
			results.add(new Result(title, success));
			if (!success)
				failed++;
		}

		public void print() {
			if (failed > 0)
				System.out.println("*** TEST FAILED (" + failed + "/" + results.size() + ") ***");
			else
				System.out.println("*** ვაიმეე TEST PASSED (" + results.size() + ") ვაიმეე ***");
			int index = 1;
			for (Result res : results) {
				res.print(index++);
			}
		}
	}

	class Result {
		private String title;
		private boolean success;

		public Result(String title, boolean success) {
			this.title = title;
			this.success = success;
		}

		public String toString() {
			if (success)
				title = title + " [PASSED]";
			else
				title = title + " [FAILED]";
			return title;
		}

		public void print(int index) {
			if (success)
				System.out.println(index + " " + toString());
			else
				System.out.println(index + " " + toString());
		}
	}

	public boolean updateTest(String id, boolean secure) {

		notificationReceived = false;

		String sparql = appProfile.getSPARQLUpdate(id);

		if (!secure)
			System.out.println("UPDATE: " + sparql);
		else
			System.out.println("SECURE UPDATE: " + sparql);

		Response response;
		if (secure)
			response = client.secureUpdate(sparql, null);
		else
			response = client.update(sparql, null);

		System.out.println(response.toString());

		return response.isUpdateResponse();
	}

	public boolean queryTest(String id, int number, boolean secure) {
		String sparql = appProfile.getSPARQLQuery(id);

		if (!secure)
			System.out.println("QUERY: " + sparql);
		else
			System.out.println("SECURE QUERY: " + sparql);

		Response response;
		if (!secure)
			response = client.query(sparql, null);
		else
			response = client.secureQuery(sparql, null);

		System.out.println(response.toString());

		if (response.isQueryResponse()) {
			QueryResponse queryResponse = (QueryResponse) response;
			List<Bindings> results = queryResponse.getBindingsResults().getBindings();
			return (results.size() == number);
		}

		return false;
	}

	public boolean subscribeTest(String id, long results, boolean secure) {
		String sparql = appProfile.getSPARQLQuery(id);

		if (secure)
			System.out.println("SECURE SUBSCRIBE: " + sparql);
		else
			System.out.println("SUBSCRIBE: " + sparql);

		Response response;

		if (!secure)
			response = client.subscribe(sparql, null);
		else
			response = client.secureSubscribe(sparql, null);

		System.out.println(response.toString());

		if (response.isSubscribeResponse()) {
			spuid = ((SubscribeResponse) response).getSpuid();
			return ((SubscribeResponse) response).getBindingsResults().size() == results;
		}

		return false;
	}
	
	public boolean waitTokenToExpire() {
		try {
			System.out.println("Wait token to expire");
			Thread.sleep(client.getTokenExpiringSeconds());
		} catch (InterruptedException | SEPASecurityException e) {
			return false;
		}
		
		return true;
	}

	public boolean waitNotification() {
		synchronized (sync) {
			if (notificationReceived)
				return true;
			try {
				System.out.println("Wait notification");
				sync.wait(notificationMaxDelay);
			} catch (InterruptedException e) {
				System.out.println(e.getMessage());
			}
		}

		return notificationReceived;
	}

	public boolean unsubscribeTest(String spuid, boolean secure) {
		Response response;
		
		if (secure)
			System.out.println("SECURE UNSUBSCRIBE: " + spuid);
		else
			System.out.println("UNSUBSCRIBE: " + spuid);
		
		if (!secure)
			response = client.unsubscribe(spuid);
		else
			response = client.secureUnsubscribe(spuid);

		System.out.println(response.toString());

		return response.isUnsubscribeResponse();
	}

	public boolean registrationTest(String id) {
		Response response;
		
		System.out.println("REGISTER: " + id);
		
		response = client.register(id);
		
		System.out.println(response.toString());
		
		return !response.getClass().equals(ErrorResponse.class);
	}

	public boolean requestAccessTokenTest() {
		Response response;
		
		System.out.println("REQUEST ACCESS TOKEN");
		
		response = client.requestToken();

		System.out.println(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	public void run() {
		results = new Results();
		
		boolean result = false;
		String id = "";
		boolean secure = false;
		boolean passed = true;
		String name  = "";
		String type = "";
		
		for (JsonElement test : sequence) {
			type = test.getAsJsonObject().get("type").getAsString();
			switch (type) {
			case "UPDATE":
			case "QUERY":
			case "SUBSCRIBE":
				id = test.getAsJsonObject().get("id").getAsString();
				secure = test.getAsJsonObject().get("secure").getAsBoolean();
				passed = test.getAsJsonObject().get("passed").getAsBoolean();
				name = test.getAsJsonObject().get("name").getAsString();
				
				if (type.equals("UPDATE")) result = updateTest(id,secure);
				else if (type.equals("QUERY")) result = queryTest(id,test.getAsJsonObject().get("results").getAsInt(),secure);
				else result = subscribeTest(id,test.getAsJsonObject().get("results").getAsInt(),secure); 
					
				if (passed) {
					results.addResult(name, result);
				} else {
					results.addResult(name, !result);
				}
				break;
			case "UNSUBSCRIBE":
				secure = test.getAsJsonObject().get("secure").getAsBoolean();
				passed = test.getAsJsonObject().get("passed").getAsBoolean();
				name = test.getAsJsonObject().get("name").getAsString();
				
				result = unsubscribeTest(spuid,secure);
				
				if (passed) {
					results.addResult(name, result);
				} else {
					results.addResult(name, !result);
				}
				break;
			case "REGISTER":
				id = test.getAsJsonObject().get("id").getAsString();
				passed = test.getAsJsonObject().get("passed").getAsBoolean();
				name = test.getAsJsonObject().get("name").getAsString();
				
				result = registrationTest(id);
				
				if (passed) {
					results.addResult(name, result);
				} else {
					results.addResult(name, !result);
				}
				break;
			case "GET_TOKEN":
				passed = test.getAsJsonObject().get("passed").getAsBoolean();
				name = test.getAsJsonObject().get("name").getAsString();
				
				result = requestAccessTokenTest();
				
				if (passed) {
					results.addResult(name, result);
				} else {
					results.addResult(name, !result);
				}
				break;
			case "WAIT_TOKEN_TO_EXPIRE":
				result = waitTokenToExpire();
				results.addResult("Wait token to expire", result);
				break;
			case "WAIT_NOTIFICATION":
				passed = test.getAsJsonObject().get("passed").getAsBoolean();
				name = test.getAsJsonObject().get("name").getAsString();
				
				result = waitNotification();
				
				if (passed) {
					results.addResult(name, result);
				} else {
					results.addResult(name, !result);
				}
				break;
			}
		}

		results.print();
	}
	
	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException { 
		System.out.println("**********************************************************");
		System.out.println("***     SPARQL 1.1 SE Protocol Service test suite      ***");
		System.out.println("**********************************************************");
		System.out.println("***   WARNING: the RDF store content will be ERASED    ***");
		System.out.println("***         Do you want to continue (yes/no)?          ***");
		System.out.println("**********************************************************");
		Scanner scanner = new Scanner(System.in);
		scanner.useDelimiter("\\n"); // "\\z" means end of input
		String input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			System.out.println("Bye bye! :-)");
			System.exit(0);
		}
		System.out.println("**********************************************************");
		System.out.println("***                Are you sure (yes/no)?              ***");
		System.out.println("**********************************************************");
		input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			System.out.println("Bye bye! :-)");
			System.exit(0);
		}
		scanner.close();
		
		SEPATestClient test = new SEPATestClient(new ApplicationProfile("sepatest.jsap"));
		test.run();
		
		//test = new SEPATestClient(new ApplicationProfile("sepatest-secure.jsap"));
		//test.run();
		
		System.exit(0);
	}
}
