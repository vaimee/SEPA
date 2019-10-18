package it.unibo.arces.wot.sepa.engine.dependability;

import javax.net.ssl.SSLContext;

import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProcessingException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.engine.gates.Gate;
import it.unibo.arces.wot.sepa.engine.processing.Processor;

public class Dependability {
	protected static Logger logger = LogManager.getLogger();
	
	private static boolean isSecure = false;

	private static AuthorizationManager authManager;
	
	public static boolean isSecure() {
		return isSecure;
	}
	
	public static void enableSecurity(String host, int port, String base, String uid, String pwd,String keystoreFileName,String keystorePwd,String keyAlias,String keyPwd,String certificate) throws SEPASecurityException {
		authManager = new AuthorizationManager(host,port,base,uid,pwd,keystoreFileName, keystorePwd, keyAlias, keyPwd, certificate);
		isSecure = true;
	}
	
	public static void enableSecurity(String keystoreFileName,String keystorePwd,String keyAlias,String keyPwd,String certificate) throws SEPASecurityException {
		authManager = new AuthorizationManager(keystoreFileName,keystorePwd,keyAlias,keyPwd, certificate);
		isSecure = true;
	}
	
	public static void setProcessor(Processor p) {
		SubscriptionManager.setProcessor(p);
	}
	
	public static AuthorizationResponse validateToken(String jwt) throws SEPASecurityException {
		return authManager.validateToken(jwt);
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
		return authManager.getSSLContext();
	}

	public static Response getToken(String encodedCredentials) {
		return authManager.getToken(encodedCredentials);
	}

	public static Response register(String identity){
		return authManager.register(identity);
	}

	public static boolean processCORSRequest(HttpAsyncExchange exchange) {
		return CORSManager.processCORSRequest(exchange);
	}

	public static boolean isPreFlightRequest(HttpAsyncExchange exchange) {
		return CORSManager.isPreFlightRequest(exchange);
	}

	

}
