package it.unibo.arces.wot.sepa.engine.bean;

import java.util.HashMap;

public class AuthorizationManagerBeans {

	private static HashMap<String,Boolean> authorizedIdentities = new HashMap<String,Boolean>();
	
	private static long expiring = 5; 												
	
	private static String issuer = "https://wot.arces.unibo.it:8443/oauth/token"; 		
	private static String httpsAudience = "https://wot.arces.unibo.it:8443/sparql"; 	
	private static String wssAudience ="wss://wot.arces.unibo.it:9443/sparql";  		
	private static String subject = "SEPATest";
	
	public static long getTokenExpiringPeriod() {
		return expiring;
	}
	
	public static void setTokenExpiringPeriod(long period) {
		expiring = period;
	}

	public static void addAuthorizedIdentity(String id) {
		authorizedIdentities.put(id, true);
	}

	public static void removeAuthorizedIdentity(String id) {
		authorizedIdentities.remove(id);
	}

	public static HashMap<String, Boolean> getAuthorizedIdentities() {
		return authorizedIdentities;
	}

	public static String getIssuer() {
		return issuer;
	}

	public static void setIssuer(String is) {
		issuer = is;
	}

	public static String getHttpsAudience() {
		return httpsAudience;
	}


	public static void setHttpsAudience(String audience) {
		httpsAudience = audience;
	}

	public static String getWssAudience() {
		return wssAudience;
	}

	public static void setWssAudience(String audience) {
		wssAudience = audience;
	}

	public static String getSubject() {
		return subject;
	}

	public static void setSubject(String sub) {
		subject = sub;
	}
}
