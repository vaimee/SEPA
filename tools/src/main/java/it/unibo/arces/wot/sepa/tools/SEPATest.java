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

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.api.INotificationHandler;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProperties;
import it.unibo.arces.wot.sepa.api.SPARQL11SEProtocol;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.request.QueryRequest;
import it.unibo.arces.wot.sepa.commons.request.SubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UnsubscribeRequest;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Notification;
import it.unibo.arces.wot.sepa.commons.response.QueryResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.response.UnsubscribeResponse;

public class SEPATest {
	protected static final Logger logger = LogManager.getLogger("SEPATest");
	protected static Results results = new SEPATest().new Results();

	// Subscription variables
	protected static boolean subscribeConfirmReceived = false;
	protected static boolean unsubscriptionConfirmReceived = false;
	protected static String spuid = null;
	protected static boolean pingReceived = false;
	protected static boolean notificationReceived = false;
	protected static Object sync = new Object();

	//SPARQL 1.1 SE Protocol client
	protected static SPARQL11SEProtocol client;
	protected static SPARQL11SEProperties properties;
	
	//Subscriptions handler
	protected static TestNotificationHandler handler = new TestNotificationHandler();

	protected static final long subscribeConfirmDelay = 2000;
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
				logger.warn("*** ვაიმეე TEST PASSED (" + results.size() + ") ვაიმეე ***");
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
				logger.warn(index + " " + toString());
			else
				logger.error(index + " " + toString());
		}
	}

	protected static class TestNotificationHandler implements INotificationHandler {

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
		public void onSubscribeConfirm(SubscribeResponse response) {
			synchronized (sync) {
				logger.debug(response.toString());
				spuid = response.getSpuid();
				subscribeConfirmReceived = true;
				sync.notify();
			}

		}

		@Override
		public void onUnsubscribeConfirm(UnsubscribeResponse response) {
			synchronized (sync) {
				logger.debug(response.toString());
				unsubscriptionConfirmReceived = true;
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

	protected static boolean updateTest(String sparql, boolean secure)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		notificationReceived = false;

		UpdateRequest update = new UpdateRequest(sparql);

		if (!secure)
			logger.info(update.toString());
		else
			logger.info("SECURE " + update.toString());

		Response response;
		if (secure)
			response = client.secureUpdate(update);
		else
			response = client.update(update);

		logger.debug(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	protected static boolean queryTest(String sparql, String utf8, boolean secure)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		QueryRequest query = new QueryRequest(sparql);

		if (!secure)
			logger.info(query.toString());
		else
			logger.info("SECURE " + query.toString());

		Response response;
		if (!secure)
			response = client.query(query);
		else
			response = client.secureQuery(query);

		logger.debug(response.toString());

		boolean error = response.getClass().equals(ErrorResponse.class);

		if (!error && utf8 != null) {
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

		return !error;
	}

	protected static boolean subscribeTest(String sparql, boolean secure)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		subscribeConfirmReceived = false;
		notificationReceived = false;

		SubscribeRequest sub = new SubscribeRequest(sparql);

		if (secure)
			logger.info("SECURE " + sub.toString());
		else
			logger.info(sub.toString());

		Response response;

		if (!secure)
			response = client.subscribe(sub);
		else
			response = client.secureSubscribe(sub);

		logger.debug(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	protected static boolean waitSubscribeConfirm() {
		synchronized (sync) {
			if (subscribeConfirmReceived)
				return true;
			try {
				sync.wait(subscribeConfirmDelay);
			} catch (InterruptedException e) {
				logger.info("InterruptedException: " + e.getMessage());
			} catch (IllegalStateException e) {
				logger.error("IllegalStateException: " + e.getMessage());
			} catch (IllegalMonitorStateException e) {
				logger.error("IllegalMonitorStateException: " + e.getMessage());
			}
		}

		return (subscribeConfirmReceived);
	}

	protected static boolean waitPing() {
		long delay = pingDelay + (pingDelay / 2);
		synchronized (sync) {
			pingReceived = false;
			try {
				logger.debug("Waiting ping in " + delay + " ms...");
				sync.wait(delay);
			} catch (InterruptedException e) {
				logger.info(e.getMessage());
			}
		}

		return pingReceived;
	}

	protected static boolean waitNotification() {
		synchronized (sync) {
			if (notificationReceived)
				return true;
			try {
				sync.wait(subscribeConfirmDelay);
			} catch (InterruptedException e) {
				logger.info(e.getMessage());
			}
		}

		return notificationReceived;
	}

	protected static boolean unsubscribeTest(String spuid, boolean secure)
			throws IOException, URISyntaxException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		unsubscriptionConfirmReceived = false;

		UnsubscribeRequest unsub = new UnsubscribeRequest(spuid);

		Response response;
		if (!secure)
			response = client.unsubscribe(unsub);
		else
			response = client.secureUnsubscribe(unsub);

		logger.debug(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	protected static boolean waitUnsubscribeConfirm() {
		synchronized (sync) {
			if (unsubscriptionConfirmReceived)
				return true;
			try {
				sync.wait(2000);
			} catch (InterruptedException e) {
				logger.debug("InterruptedException: " + e.getMessage());
			}
		}

		return unsubscriptionConfirmReceived;
	}

	protected static boolean registrationTest(String id) throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		Response response;
		response = client.register(id);
		return !response.getClass().equals(ErrorResponse.class);
	}

	protected static boolean requestAccessTokenTest() throws IOException, URISyntaxException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InterruptedException, UnrecoverableKeyException, KeyManagementException, KeyStoreException, CertificateException {
		Response response;
		response = client.requestToken();

		logger.debug(response.toString());

		return !response.getClass().equals(ErrorResponse.class);
	}

	public static void main(String[] args) {
		boolean ret = false;

		try {
			properties = new SPARQL11SEProperties("client.jpar");
		} catch (NoSuchElementException | IOException | InvalidKeyException | IllegalArgumentException
				| NullPointerException | ClassCastException | NoSuchAlgorithmException | NoSuchPaddingException
				| IllegalBlockSizeException | BadPaddingException e2) {
			logger.fatal("JSAP exception: " + e2.getMessage());
			System.exit(1);
		}
		try {
			client = new SPARQL11SEProtocol(properties);
		} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException
				| NoSuchAlgorithmException | CertificateException | IOException | URISyntaxException e2) {
			logger.fatal(e2.getLocalizedMessage());
			System.exit(1);
		}

		client.setNotificationHandler(handler);

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
		try {
			ret = updateTest(
					"prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"測試\"} where {?s ?p ?o}",
					false);
		} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Update", ret);
		if (ret)
			logger.warn("Update PASSED");
		else
			logger.error("Update FAILED");

		// QUERY
		try {
			ret = queryTest("select ?o where {?s ?p ?o}", "測試", false);
		} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Query", ret);
		if (ret)
			logger.warn("Query PASSED");
		else
			logger.error("Query FAILED");

		// SUBSCRIBE
		try {
			ret = subscribeTest("select ?o where {?s ?p ?o}", false);
		} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Subscribe - request", ret);
		if (ret)
			logger.warn("Subscribe PASSED");
		else
			logger.error("Subscribe FAILED");

		// SUBSCRIBE CONFIRM
		ret = waitSubscribeConfirm();
		results.addResult("Subscribe - confirm", ret);
		if (ret)
			logger.warn("Subscribe confirmed PASSED");
		else
			logger.error("Subscribe confirmed FAILED");

		// FIRST NOTIFICATION
		ret = waitNotification();
		results.addResult("Subscribe - results", ret);
		if (ret)
			logger.warn("First results received PASSED");
		else
			logger.error("First results received FAILED");

		// PING
		ret = waitPing();
		results.addResult("Subscribe - ping", ret);
		if (ret)
			logger.warn("Ping received PASSED");
		else
			logger.error("Ping recevied FAILED");

		// TRIGGER A NOTIFICATION
		try {
			ret = updateTest(
					"prefix test:<http://www.vaimee.com/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",
					false);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Subscribe - triggering", ret);
		if (ret)
			logger.warn("Triggering update PASSED");
		else
			logger.error("Triggering update FAILED");

		// WAIT NOTIFICATION
		ret = waitNotification();
		results.addResult("Subscribe - notification", ret);
		if (ret)
			logger.warn("Notification PASSED");
		else
			logger.error("Notification FAILED");

		// UNSUBSCRIBE
		try {
			ret = unsubscribeTest(spuid, false);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Unsubscribe - request", ret);
		if (ret)
			logger.warn("Unsubscribe PASSED");
		else
			logger.error("Unsubscribe FAILED");

		// WAIT UNSUBSCRIBE CONFIRM
		ret = waitUnsubscribeConfirm();
		results.addResult("Unsubscribe - confirm", ret);
		if (ret)
			logger.warn("Unsubscribe confirmed PASSED");
		else
			logger.error("Unsubscribe confirmed FAILED");

		// PING
		ret = !waitPing();
		results.addResult("Unsubscribe - ping", ret);
		if (ret)
			logger.warn("Ping not received PASSED");
		else
			logger.error("Ping not recevied FAILED");

		// **********************
		// Enable security
		// **********************
		logger.info("Switch to secure mode");

		// REGISTRATION
		try {
			ret = registrationTest("SEPATest");
		} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Registration", ret);
		if (ret)
			logger.warn("Registration PASSED");
		else
			logger.error("Registration FAILED");

		// REGISTRATION (registration not allowed)
		try {
			ret = !registrationTest("RegisterMePlease");
		} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Registration not allowed", ret);
		if (ret)
			logger.warn("Registration not allowed PASSED");
		else
			logger.error("Registration not allowed FAILED");

		// REQUEST ACCESS TOKEN
		try {
			ret = requestAccessTokenTest();
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Access token", ret);
		if (ret)
			logger.warn("Access token PASSED");
		else
			logger.error("Access token FAILED");

		// REQUEST ACCESS TOKEN (not expired);
		if (!properties.isTokenExpired())
			try {
				ret = !requestAccessTokenTest();
			} catch (URISyntaxException | IOException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		else
			ret = false;
		results.addResult("Access token not expired", ret);
		if (ret)
			logger.warn("Access token (not expired) PASSED");
		else
			logger.error("Access token (not expired) FAILED");

		// REQUEST ACCESS TOKEN (expired);
		logger.info("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		if (properties.isTokenExpired())
			try {
				ret = requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		else
			ret = false;
		results.addResult("Access token expired", ret);
		if (ret)
			logger.warn("Access token (expired) PASSED");
		else
			logger.error("Access token (expired) FAILED");

		// SECURE UPDATE
		if (properties.isTokenExpired())
			try {
				requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		try {
			ret = updateTest(
					"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"ვაიმეე\"} where {?s ?p ?o}",
					true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure update ", ret);
		if (ret)
			logger.warn("Secure update PASSED");
		else
			logger.error("Secure update FAILED");

		// SECURE UPDATE (expired token)
		logger.info("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		try {
			ret = !updateTest(
					"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"vaimee!\"} where {?s ?p ?o}",
					true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure update (expired)", ret);
		if (ret)
			logger.warn("Secure update (expired) PASSED");
		else
			logger.error("Secure update (expired) FAILED");

		// SECURE QUERY
		if (properties.isTokenExpired())
			try {
				requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		try {
			ret = queryTest("select ?o where {?s ?p ?o}", "ვაიმეე", true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure query", ret);
		if (ret)
			logger.warn("Secure query PASSED");
		else
			logger.error("Secure query FAILED");

		// SECURE QUERY (expired token)
		logger.info("Waiting token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		try {
			ret = !queryTest("select ?o where {?s ?p ?o}", "ვაიმეე", true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure query (expired)", ret);
		if (ret)
			logger.warn("Secure query (expired) PASSED");
		else
			logger.error("Secure query (expired) FAILED");

		// SECURE SUBSCRIBE
		if (properties.isTokenExpired())
			try {
				requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		try {
			ret = subscribeTest("select ?o where {?s ?p ?o}", true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure subscribe - request", ret);
		if (ret)
			logger.warn("Secure subscribe PASSED");
		else
			logger.error("Secure subscribe FAILED");

		// SUBSCRIBE CONFIRM
		ret = waitSubscribeConfirm();
		results.addResult("Secure subscribe - confirm", ret);
		if (ret)
			logger.warn("Secure subscribe confirmed PASSED");
		else
			logger.error("Secure subscribe confirmed FAILED");

		// FIRST NOTIFICATION
		ret = waitNotification();
		results.addResult("Secure subscribe - results", ret);
		if (ret)
			logger.warn("First results received PASSED");
		else
			logger.error("First results received FAILED");

		// PING
		ret = waitPing();
		results.addResult("Secure subscribe - ping", ret);
		if (ret)
			logger.warn("Secure ping received PASSED");
		else
			logger.error("Secure ping recevied FAILED");

		// TRIGGER A NOTIFICATION
		if (properties.isTokenExpired())
			try {
				requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		try {
			ret = updateTest(
					"prefix test:<http://wot.arces.unibo.it/test#> delete {?s ?p ?o} insert {test:Sub test:Pred \"卢卡\"} where {?s ?p ?o}",
					true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure subscribe - triggering", ret);
		if (ret)
			logger.warn("Secure triggering update PASSED");
		else
			logger.error("Secure triggering update FAILED");

		// NOTIFICATION
		ret = waitNotification();
		results.addResult("Secure subscribe - notification", ret);
		if (ret)
			logger.warn("Secure subscribe - notification PASSED");
		else
			logger.error("Secure subscribe - notification FAILED");

		// SECURE UNSUBSCRIBE (expired)
		logger.info("Wait token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		try {
			ret = unsubscribeTest(spuid, true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure unsubscribe (expired) - request", ret);
		if (ret)
			logger.warn("Secure unsubscribe (expired) - request PASSED");
		else
			logger.error("Secure unsubscribe (expired) - request FAILED");

		// WAIT UNSUBSCRIBE CONFIRM
		/*
		 * ret = waitUnsubscribeConfirm();
		 * results.addResult("Secure unsubscribe (expired) - confirm", !ret); if
		 * (ret) logger.warn("Secure unsubscribe (expired) - confirm PASSED");
		 * else logger.error("Secure unsubscribe (expired) - confirm FAILED");
		 */
		// UNSUBSCRIBE
		if (properties.isTokenExpired())
			try {
				requestAccessTokenTest();
			} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
					| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
				ret = false;
				logger.error(e1.getMessage());
			}
		try {
			ret = unsubscribeTest(spuid, true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			ret = false;
			logger.error(e1.getMessage());
		}
		results.addResult("Secure unsubscribe - request", ret);
		if (ret)
			logger.warn("Secure unsubscribe - request PASSED");
		else
			logger.error("Secure unsubscribe - request FAILED");

		// WAIT UNSUBSCRIBE CONFIRM
		ret = waitUnsubscribeConfirm();
		results.addResult("Secure unsubscribe - confirm", ret);
		if (ret)
			logger.warn("Secure unsubscribe - confirm PASSED");
		else
			logger.error("Secure unsubscribe - confirm  FAILED");

		// WAITING PING
		ret = !waitPing();
		results.addResult("Secure unsubscribe - ping", ret);
		if (ret)
			logger.warn("Secure unsubscribe - ping PASSED");
		else
			logger.error("Secure unsubscribe - ping FAILED");

		// SECURE SUBSCRIBE (expired)
		logger.info("Wait token expiring in " + properties.getExpiringSeconds() + " + 2 seconds...");
		try {
			Thread.sleep((properties.getExpiringSeconds() + 2) * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
		try {
			ret = subscribeTest("select ?o where {?s ?p ?o}", true);
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException| InterruptedException | BadPaddingException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e) {
			ret = false;
			logger.error(e.getMessage());
		}
		results.addResult("Secure subscribe (expired) - request", ret);
		if (ret)
			logger.warn("Secure subscribe (expired) - request PASSED");
		else
			logger.error("Secure subscribe (expired) - request FAILED");

		results.print();
		
		System.exit(0);
	}
}
