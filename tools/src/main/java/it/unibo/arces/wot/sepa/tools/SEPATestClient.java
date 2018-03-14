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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.GenericClient;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;

public class SEPATestClient extends GenericClient {
	protected static final Logger logger = LogManager.getLogger("SEPATest");
	protected Results results;
	protected String spuid = null;
	protected boolean notificationReceived = false;
	protected Object sync = new Object();

	protected long notificationMaxDelay = 2000;
	protected JsonArray sequence;

	private ApplicationProfile appProfile;

	public SEPATestClient(ApplicationProfile appProfile) throws SEPAProtocolException, SEPASecurityException {
		super(appProfile);

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
				logger.error("*** TEST FAILED (" + failed + "/" + results.size() + ") ***");
			else
				logger.info("*** ვაიმეე TEST PASSED (" + results.size() + ") ვაიმეე ***");
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
				logger.info(index + " " + toString());
			else
				logger.error(index + " " + toString());
		}
	}

	public boolean updateTest(String id, boolean secure) {

		notificationReceived = false;

		String sparql = appProfile.update(id);

		if (!secure)
			logger.debug("UPDATE: " + sparql);
		else
			logger.debug("SECURE UPDATE: " + sparql);

		Response response;
		if (secure)
			response = secureUpdate(sparql, null);
		else
			response = update(sparql, null);

		logger.debug(response.toString());

		return response.isUpdateResponse();
	}

	public boolean queryTest(String id, int number, boolean secure) {
		String sparql = appProfile.subscribe(id);

		if (!secure)
			logger.debug("QUERY: " + sparql);
		else
			logger.debug("SECURE QUERY: " + sparql);

		Response response;
		if (!secure)
			response = query(sparql, null);
		else
			response = secureQuery(sparql, null);

		logger.debug(response.toString());

		if (response.isQueryResponse()) {
			QueryResponse queryResponse = (QueryResponse) response;
			List<Bindings> results = queryResponse.getBindingsResults().getBindings();
			return (results.size() == number);
		}

		return false;
	}

	public boolean subscribeTest(String id, long results, boolean secure) {
		String sparql = appProfile.subscribe(id);

		if (secure)
			logger.debug("SECURE SUBSCRIBE: " + sparql);
		else
			logger.debug("SUBSCRIBE: " + sparql);

		Response response;

		if (!secure)
			response = subscribe(sparql, null);
		else
			response = secureSubscribe(sparql, null);

		logger.debug(response.toString());

		if (response.isSubscribeResponse()) {
			spuid = ((SubscribeResponse) response).getSpuid();
			return ((SubscribeResponse) response).getBindingsResults().size() == results;
		}

		return false;
	}
	
	public boolean waitTokenToExpire() {
		try {
			Thread.sleep(getTokenExpiringSeconds());
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
				sync.wait(notificationMaxDelay);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		return notificationReceived;
	}

	public boolean unsubscribeTest(String spuid, boolean secure) {
		Response response;
		if (!secure)
			response = unsubscribe(spuid);
		else
			response = secureUnsubscribe(spuid);

		logger.debug(response.toString());

		return response.isUnsubscribeResponse();
	}

	public boolean registrationTest(String id) {
		Response response;
		response = register(id);
		return !response.getClass().equals(ErrorResponse.class);
	}

	public boolean requestAccessTokenTest() {
		Response response;
		response = requestToken();

		logger.debug(response.toString());

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

	@Override
	public void onSemanticEvent(Notification notify) {
		synchronized (sync) {
			logger.debug(notify.toString());
			notificationReceived = true;
			sync.notify();
		}

	}

	@Override
	public void onPing() {

	}

	@Override
	public void onBrokenSocket() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		synchronized (sync) {
			logger.debug(errorResponse.toString());
			sync.notify();
		}

	}
	
	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException { 
		logger.warn("**********************************************************");
		logger.warn("***     SPARQL 1.1 SE Protocol Service test suite      ***");
		logger.warn("**********************************************************");
		logger.warn("***   WARNING: the RDF store content will be ERASED    ***");
		logger.warn("***         Do you want to continue (yes/no)?          ***");
		logger.warn("**********************************************************");
		Scanner scanner = new Scanner(System.in);
		scanner.useDelimiter("\\n"); // "\\z" means end of input
		String input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			logger.info("Bye bye! :-)");
			System.exit(0);
		}
		logger.warn("**********************************************************");
		logger.warn("***                Are you sure (yes/no)?              ***");
		logger.warn("**********************************************************");
		input = scanner.next();
		if (!input.equals("yes")) {
			scanner.close();
			logger.info("Bye bye! :-)");
			System.exit(0);
		}
		scanner.close();
		
		SEPATestClient test = new SEPATestClient(new ApplicationProfile("sepatest.jsap"));
		test.run();
		
		System.exit(0);
	}
}
