package it.unibo.arces.wot.sepa.engine.bean;

import java.time.Duration;
import java.util.Date;

public class EngineBeans {
	private static Date startDate = new Date();
	
	private static String queryURL;
	private static String updateURL;
	
	private static String secureQueryURL;
	private static String secureUpdateURL;
	private static String registrationURL;
	private static String tokenRequestURL;
	
	private static String version;
	
	public static void setVersion(String v) {
		version = v;
	}
	public static String getVersion() {
		return version;
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
	public static void resetAll() {
		ProcessorBeans.reset();
		SchedulerBeans.reset();
		SubscribeProcessorBeans.reset();
	}
}
