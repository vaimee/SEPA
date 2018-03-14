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
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.ISubscriptionHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;

public class SEPATest {
	protected static final Logger logger = LogManager.getLogger("SEPATest");
	protected static Results results = new SEPATest().new Results();

	// Subscription variables
	protected static String spuid = null;
	protected static boolean pingReceived = false;
	protected static boolean notificationReceived = false;
	protected static Object sync = new Object();

	// SPARQL 1.1 SE Protocol client
	protected static SPARQL11SEProtocol client;
	protected static SPARQL11SEProperties properties;

	// Subscriptions handler
	protected static TestNotificationHandler handler = new TestNotificationHandler();

	protected static final long notificationMaxDelay = 2000;
	protected static final long pingDelay = 5000;

	protected class Results {
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

	protected class Result {
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

	protected static class TestNotificationHandler implements ISubscriptionHandler {

		@Override
		public void onBrokenSocket() {
			logger.debug("Broken subscription");
		}

		@Override
		public void onError(ErrorResponse errorResponse) {
			synchronized (sync) {
				logger.debug(errorResponse.toString());
				sync.notify();
			}
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
			synchronized (sync) {
				logger.debug(new Date() + " Ping");
				pingReceived = true;
				sync.notify();
			}
		}
	}

	protected static boolean updateTest(String sparql, boolean secure) {

		notificationReceived = false;

		UpdateRequest update = new UpdateRequest(sparql);

		if (!secure)
			logger.debug(update.toString());
		else
			logger.debug("SECURE " + update.toString());

		Response response;
		if (secure)
			response = client.secureUpdate(update);
		else
			response = client.update(update);

		logger.debug(response.toString());

		return response.isUpdateResponse();
	}

	protected static boolean queryTest(String sparql, String utf8, boolean secure) {
		QueryRequest query = new QueryRequest(sparql);

		if (!secure)
			logger.debug(query.toString());
		else
			logger.debug("SECURE " + query.toString());

		Response response;
		if (!secure)
			response = client.query(query);
		else
			response = client.secureQuery(query);

		logger.debug(response.toString());

		if (response.isQueryResponse() && utf8 != null) {
			QueryResponse queryResponse = (QueryResponse) response;
			List<Bindings> results = queryResponse.getBindingsResults().getBindings();
			if (results.size() == 1) {
				Bindings bindings = results.get(0);
				if (bindings.isLiteral("o")) {
					String value = bindings.getBindingValue("o");
					if (value.equals(utf8))
						return true;
				}
			}

			return false;
		}

		return response.isQueryResponse();
	}

	protected static boolean subscribeTest(String sparql, boolean secure) {

		SubscribeRequest sub = new SubscribeRequest(sparql);

		if (secure)
			logger.debug("SECURE " + sub.toString());
		else
			logger.debug(sub.toString());

		Response response;

		if (!secure)
			response = client.subscribe(sub);
		else
			response = client.secureSubscribe(sub);

		logger.debug(response.toString());

		if (response.isSubscribeResponse()) {
			spuid = ((SubscribeResponse) response).getSpuid();
			return true;
		}

		return false;
	}

	protected static boolean waitPing() {
		long delay = pingDelay + (pingDelay / 2);
		synchronized (sync) {
			pingReceived = false;
			try {
				logger.debug("Waiting ping in " + delay + " ms...");
				sync.wait(delay);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
			}
		}

		return pingReceived;
	}

	protected static boolean waitNotification() {
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

	protected static boolean unsubscribeTest(String spuid, boolean secure) {

		UnsubscribeRequest unsub = new UnsubscribeRequest(spuid);

		Response response;
		if (!secure)
			response = client.unsubscribe(unsub);
		else
			response = client.secureUnsubscribe(unsub);

		logger.debug(response.toString());

		return response.isUnsubscribeResponse();
	}

	protected static boolean registrationTest(String id) {
		Response response;
		response = client.register(id);
		return !response.getClass().equals(ErrorResponse.class);
	}

	protected static boolean requestAccessTokenTest() {
		Response response;
		response = client.requestToken();

		logger.debug(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	public static void main(String[] args) throws SEPASecurityException {
		boolean ret = false;

		try {
			properties = new SPARQL11SEProperties("client.jpar");
		} catch (SEPAPropertiesException e2) {
			logger.fatal("JSAP exception: " + e2.getMessage());
			System.exit(1);
		}
		try {
			client = new SPARQL11SEProtocol(properties,handler);
		} catch (SEPAProtocolException  e2) {
			logger.fatal(e2.getLocalizedMessage());
			System.exit(1);
		}

		logger.warn("**********************************************************");
		logger.warn("***     SPARQL 1.1 SE Protocol Service test suite      ***");
		logger.warn("**********************************************************");
		logger.warn("***   WARNING: the RDF store content will be ERASED    ***");
		logger.warn("***         Do you want to continue (yes/no)?          ***");
		logger.warn("**********************************************************");
		logger.warn("SPARQL 1.1 SE Protocol Service parameters: " + client.toString());
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

		// UPDATE
		ret = updateTest(
				"prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"測試\"} where {?s ?p ?o}",
				false);

		results.addResult("Update", ret);
		if (ret)
			logger.info("Update PASSED");
		else
			logger.error("Update FAILED");

		// QUERY
		ret = queryTest("select ?o where {?s ?p ?o}", "測試", false);

		results.addResult("Query", ret);
		if (ret)
			logger.info("Query PASSED");
		else
			logger.error("Query FAILED");

		// SUBSCRIBE

		ret = subscribeTest("select ?o where {?s ?p ?o}", false);

		results.addResult("Subscribe - request", ret);
		if (ret)
			logger.info("Subscribe PASSED");
		else
			logger.error("Subscribe FAILED");

		// PING
//		ret = waitPing();
//		results.addResult("Subscribe - ping", ret);
//		if (ret)
//			logger.info("Ping received PASSED");
//		else
//			logger.error("Ping recevied FAILED");

		// TRIGGER A NOTIFICATION
		ret = updateTest(
				"prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",
				false);

		results.addResult("Subscribe - triggering", ret);
		if (ret)
			logger.info("Triggering update PASSED");
		else
			logger.error("Triggering update FAILED");

		// WAIT NOTIFICATION
		ret = waitNotification();
		results.addResult("Subscribe - notification", ret);
		if (ret)
			logger.info("Notification PASSED");
		else
			logger.error("Notification FAILED");

		// UNSUBSCRIBE
		ret = unsubscribeTest(spuid, false);

		results.addResult("Unsubscribe - request", ret);
		if (ret)
			logger.info("Unsubscribe PASSED");
		else
			logger.error("Unsubscribe FAILED");

		// PING
//		ret = !waitPing();
//		results.addResult("Unsubscribe - ping", ret);
//		if (ret)
//			logger.info("Ping not received PASSED");
//		else
//			logger.error("Ping not recevied FAILED");

		// **********************
		// Enable security
		// **********************
		logger.debug("Switch to secure mode");

		// REGISTRATION (registration not allowed)
		ret = !registrationTest("RegisterMePlease");

		results.addResult("Registration not allowed", ret);
		if (ret)
			logger.info("Registration not allowed PASSED");
		else
			logger.error("Registration not allowed FAILED");
		
		// REGISTRATION
		ret = registrationTest("SEPATest");

		results.addResult("Registration", ret);
		if (ret)
			logger.info("Registration PASSED");
		else
			logger.error("Registration FAILED");

		// REQUEST ACCESS TOKEN
		ret = requestAccessTokenTest();
		results.addResult("Access token", ret);
		if (ret)
			logger.info("Access token PASSED");
		else
			logger.error("Access token FAILED");

		// REQUEST ACCESS TOKEN (not expired);
		try {
			if (!properties.isTokenExpired())
				ret = !requestAccessTokenTest();
			else
				ret = false;
		} catch (SEPASecurityException e2) {
			logger.error(e2.getMessage());
			ret = false;
		}
		results.addResult("Access token not expired", ret);
		if (ret)
			logger.info("Access token (not expired) PASSED");
		else
			logger.error("Access token (not expired) FAILED");

		// REQUEST ACCESS TOKEN (expired);
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e2) {
			logger.error(e2.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				ret = requestAccessTokenTest();
			else
				ret = false;
		} catch (SEPASecurityException e2) {
			logger.error(e2.getMessage());
			ret = false;
		}
		results.addResult("Access token expired", ret);
		if (ret)
			logger.info("Access token (expired) PASSED");
		else
			logger.error("Access token (expired) FAILED");

		// SECURE UPDATE
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e2) {
			logger.error(e2.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				requestAccessTokenTest();
		} catch (SEPASecurityException e13) {
			logger.error(e13.getMessage());
		}

		ret = updateTest(
				"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",
				true);

		results.addResult("Secure update ", ret);
		if (ret)
			logger.info("Secure update PASSED");
		else
			logger.error("Secure update FAILED");

		// SECURE UPDATE (expired token)
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e12) {
			logger.error(e12.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}

		ret = !updateTest(
				"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"vaimee!\"} where {?s ?p ?o}",
				true);

		results.addResult("Secure update (expired)", ret);
		if (ret)
			logger.info("Secure update (expired) PASSED");
		else
			logger.error("Secure update (expired) FAILED");

		// SECURE QUERY
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e11) {
			logger.error(e11.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				requestAccessTokenTest();
		} catch (SEPASecurityException e10) {
			logger.error(e10.getMessage());
		}

		ret = queryTest("select ?o where {?s ?p ?o}", "ვაიმეე", true);

		results.addResult("Secure query", ret);
		if (ret)
			logger.info("Secure query PASSED");
		else
			logger.error("Secure query FAILED");

		// SECURE QUERY (expired token)
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e9) {
			logger.error(e9.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}

		ret = !queryTest("select ?o where {?s ?p ?o}", "ვაიმეე", true);

		results.addResult("Secure query (expired)", ret);
		if (ret)
			logger.info("Secure query (expired) PASSED");
		else
			logger.error("Secure query (expired) FAILED");

		// SECURE SUBSCRIBE
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e8) {
			logger.error(e8.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				requestAccessTokenTest();
		} catch (SEPASecurityException e7) {
			logger.error(e7.getMessage());
		}

		ret = subscribeTest("select ?o where {?s ?p ?o}", true);

		results.addResult("Secure subscribe - request", ret);
		if (ret)
			logger.info("Secure subscribe PASSED");
		else
			logger.error("Secure subscribe FAILED");

		// PING
		ret = waitPing();
		results.addResult("Secure subscribe - ping", ret);
		if (ret)
			logger.info("Secure ping received PASSED");
		else
			logger.error("Secure ping recevied FAILED");

		// TRIGGER A NOTIFICATION
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e6) {
			logger.error(e6.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				requestAccessTokenTest();
		} catch (SEPASecurityException e5) {
			logger.error(e5.getMessage());
		}

		ret = updateTest(
				"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"卢卡\"} where {?s ?p ?o}",
				true);

		results.addResult("Secure subscribe - triggering", ret);
		if (ret)
			logger.info("Secure triggering update PASSED");
		else
			logger.error("Secure triggering update FAILED");

		// NOTIFICATION
		ret = waitNotification();
		results.addResult("Secure subscribe - notification", ret);
		if (ret)
			logger.info("Secure subscribe - notification PASSED");
		else
			logger.error("Secure subscribe - notification FAILED");

		// SECURE UNSUBSCRIBE (expired)
		try {
			logger.debug("Wait token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e4) {
			logger.error(e4.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}

		ret = !unsubscribeTest(spuid, true);

		results.addResult("Secure unsubscribe (expired) - request", ret);

		if (ret)
			logger.info("Secure unsubscribe (expired) - request PASSED");
		else
			logger.error("Secure unsubscribe (expired) - request FAILED");

		// UNSUBSCRIBE
		try {
			logger.debug("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e3) {
			logger.error(e3.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}
		try {
			if (properties.isTokenExpired())
				requestAccessTokenTest();
		} catch (SEPASecurityException e2) {
			logger.error(e2.getMessage());
		}

		ret = unsubscribeTest(spuid, true);

		results.addResult("Secure unsubscribe - request", ret);
		if (ret)
			logger.info("Secure unsubscribe - request PASSED");
		else
			logger.error("Secure unsubscribe - request FAILED");

		// WAITING PING
		ret = !waitPing();
		results.addResult("Secure unsubscribe - ping", ret);
		if (ret)
			logger.info("Secure unsubscribe - ping PASSED");
		else
			logger.error("Secure unsubscribe - ping FAILED");

		// SECURE SUBSCRIBE (expired)
		try {
			logger.info("Wait token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		} catch (SEPASecurityException e1) {
			logger.error(e1.getMessage());
		}
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException | SEPASecurityException e) {
			logger.error(e.getMessage());
		}

		ret = !subscribeTest("select ?o where {?s ?p ?o}", true);

		results.addResult("Secure subscribe (expired) - request", ret);
		if (ret)
			logger.info("Secure subscribe (expired) - request PASSED");
		else
			logger.error("Secure subscribe (expired) - request FAILED");

		results.print();

		System.exit(0);
	}
}
