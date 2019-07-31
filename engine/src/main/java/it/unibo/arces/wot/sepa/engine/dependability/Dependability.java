package it.unibo.arces.wot.sepa.engine.dependability;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.gates.Gate;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

public class Dependability {
	protected static Logger logger = LogManager.getLogger();
	
	private static boolean isSecure = false;

	public static boolean isSecure() {
		return isSecure;
	}
	
	public static void enableSecurity(String keystoreFileName,String keystorePwd,String keyAlias,String keyPwd,String certificate) throws SEPASecurityException {
		try {
			AuthorizationManager.init(keystoreFileName, keystorePwd, keyAlias, keyPwd, certificate);
		} catch (UnrecoverableKeyException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException
				| CertificateException | IOException | JOSEException | SEPASecurityException e) {
			logger.error(e.getMessage());
			throw new SEPASecurityException(e);
		}
		isSecure = true;
	}
	
	public static void setProcessor(Processor p) {
		SubscriptionManager.setProcessor(p);
	}
	
	public static Response validateToken(String jwt) {
		return AuthorizationManager.validateToken(jwt);
	}

	public static void onCloseGate(String gid) throws SEPAProcessingException {
		SubscriptionManager.onClose(gid);
	}

	public static void addGate(Gate g)  {
		SubscriptionManager.addGate(g);
	}
	
	public static void removeGate(Gate g)  {
		SubscriptionManager.removeGate(g);
	}
	
	public static void onGateError(String gid, Exception e) {
		SubscriptionManager.onError(gid, e);
	}

	public static void onSubscribe(String gid, String sid) {
		SubscriptionManager.onSubscribe(gid, sid);
	}

	public static void onUnsubscribe(String gid, String sid) {
		SubscriptionManager.onUnsubscribe(gid, sid);
	}

	public static SSLContext getSSLContext() throws SEPASecurityException {
		return AuthorizationManager.getSSLContext();
	}

	public static Response getToken(String encodedCredentials) {
		return AuthorizationManager.getToken(encodedCredentials);
	}

	public static Response register(String identity) {
		return AuthorizationManager.register(identity);
	}

	public static boolean processCORSRequest(HttpAsyncExchange exchange) {
		return CORSManager.processCORSRequest(exchange);
	}

	public static boolean isPreFlightRequest(HttpAsyncExchange exchange) {
		return CORSManager.isPreFlightRequest(exchange);
	}

	

}
