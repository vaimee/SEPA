package it.unibo.arces.wot.sepa.engine.bean;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Date;

import it.unibo.arces.wot.sepa.engine.core.EngineProperties;

public class EngineBeans {
	private static Date startDate = new Date();

	private static EngineProperties properties;

	private static String version;

	public static void setVersion(String v) {
		version = v;
	}

	public static String getVersion() {
		return version;
	}

	public static void setEngineProperties(EngineProperties prop) {
		properties = prop;
	}

	public static String getQueryPath() {
		return properties.getQueryPath();
	}

	public static String getUpdatePath() {
		return properties.getUpdatePath();
	}

	public static String getSubscribePath() {
		return properties.getSubscribePath();
	}

	public static String getSecurePath() {
		return properties.getSecurePath();
	}

	public static String getRegisterPath() {
		return properties.getRegisterPath();
	}

	public static String getTokenRequestPath() {
		return properties.getTokenRequestPath();
	}

	public static int getHttpPort() {
		return properties.getHttpPort();
	}

	public static int getHttpsPort() {
		return properties.getHttpsPort();
	}

	public static int getWsPort() {
		return properties.getWsPort();
	}

	public static int getWssPort() {
		return properties.getWssPort();
	}

	public static boolean getSecure() {
		return properties.isSecure();
	}

	public static String getUpTime() {
		return startDate.toString() + " " + Duration.between(startDate.toInstant(), new Date().toInstant()).toString();
	}

	public static void resetAll() {
		QueryProcessorBeans.reset();
		UpdateProcessorBeans.reset();
		SchedulerBeans.reset();
		SubscribeProcessorBeans.reset();
	}

	public static String getHost() {
		try {
			return Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "localhost";
		}
	}
	
	public static String getQueryURL() {
		String port = "";
		if (getHttpPort() != -1) port = ":"+getHttpPort();
		return "http://"+getHost()+port+getQueryPath();
	}

	public static String getUpdateURL() {
		String port = "";
		if (getHttpPort() != -1) port = ":"+getHttpPort();
		return "http://"+getHost()+port+getUpdatePath();
	}

	public static String getSecureQueryURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getQueryPath();
	}

	public static String getSecureUpdateURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getSecurePath()+getUpdatePath();
	}

	public static String getRegistrationURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getRegisterPath();
	}

	public static String getTokenRequestURL() {
		String port = "";
		if (getHttpsPort() != -1) port = ":"+getHttpsPort();
		return "https://"+getHost()+port+getTokenRequestPath();
	}
}
