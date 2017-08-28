package it.unibo.arces.wot.sepa.engine.bean;

import java.time.Duration;
import java.util.Date;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

public class EngineBeans {
	private static Date startDate = new Date();
	private static long timeout;
	private static long keepalive;
	
	private static String queryURL;
	private static String updateURL;
	
	private static String secureQueryURL;
	private static String secureUpdateURL;
	private static String registrationURL;
	private static String tokenRequestURL;
	
	public static void setEngineProperties(EngineProperties prop) {	
		timeout = prop.getTimeout();
		keepalive = prop.getKeepAlivePeriod();
	}
	
	public static void setTimeout(long t) {
		timeout = t;
	}
	public static long getTimeout() {
		return timeout;
	}
	
	public static void setKeepalive(long t) {
		keepalive = t;
	}
	public static long getKeepalive() {
		return keepalive;
	}
	
	public static void setQueryURL(String s) {
		queryURL = s;
	}
	public static void setSecureQueryURL(String s) {
		secureQueryURL = s;
	}
	
	public static String getQueryURL() {
		return queryURL;
	}
	
	public static String getSecureQueryURL() {
		return secureQueryURL;
	}
	
	public  static void setUpdateURL(String s) {
		updateURL = s;	
	}
	
	public  static void setSecureUpdateURL(String s) {
		secureUpdateURL = s;	
	}
	
	public static String getUpdateURL() {
		return updateURL;
	}
	
	public static String getSecureUpdateURL() {
		return secureUpdateURL;
	}
	
	public static void setRegistrationURL(String string) {
		registrationURL = string;
	}
	public static String getRegistrationURL() {
		return registrationURL;
	}
	
	public  static void setTokenRequestURL(String string) {
		tokenRequestURL = string;
	}
	public static String getTokenRequestURL() {
		return tokenRequestURL;
	}
	
	public static String getUpTime() {
		return startDate.toString() + " " + Duration.between(startDate.toInstant(), new Date().toInstant()).toString();
	}
}
