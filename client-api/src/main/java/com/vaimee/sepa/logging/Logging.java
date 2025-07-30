package com.vaimee.sepa.logging;

import java.io.File;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import com.vaimee.sepa.api.commons.request.Request;
import com.vaimee.sepa.api.commons.response.Response;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Logging {
	private static void initLoggerFromJarDir() {
		try {
			File jarFile = new File(Logging.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			File jarDir = jarFile.getParentFile();

			File configFile = new File(jarDir, "log4j2.xml");
			if (configFile.exists()) {
				Configurator.initialize(null, configFile.getAbsolutePath());
				System.out.println("| Log4j2 configured from: " + configFile.getAbsolutePath());
			} else {
				System.err.println("|- log4j2.xml not found: " + configFile.getAbsolutePath());
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static void init() {
		System.out.println("");
		System.out.println("|------------ Logging ------------");

		initLoggerFromJarDir();

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); // Quoted "Z" to indicate GMT, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		System.setProperty("logFilename", nowAsISO);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();

		final Configuration config = ctx.getConfiguration();
		Map<String,LoggerConfig> loggers = config.getLoggers();
		for (String key : loggers.keySet()) {
			Iterator<AppenderRef> it = loggers.get(key).getAppenderRefs().iterator();
			while (it.hasNext()) {
				AppenderRef ref = it.next();
				System.out.println("| Appender: <" + ref.getRef() + "> Level: " + ref.getLevel());
			}
		}
		
		System.out.println("| Logger level: " + LogManager.getLogger().getLevel().toString());
		System.out.println("|---------------------------------");
		System.out.println("");
	}

	public static long getTime() {
		return System.nanoTime();
	}

	public synchronized static void logTiming(String tag, long start, long stop) {
		String message = String.format("%d,%d,%d,%d,%s",System.currentTimeMillis(),(stop-start)/1000000,(stop-start)/1000,stop-start,tag);
		LogManager.getLogger().log(Level.getLevel("timing"),message);
	}

	public synchronized static void logTiming(Request request) {
		long start = getTime();

		String tag;
		if (request.isUpdateRequest()) tag = "UPDATE_REQUEST";
		else if (request.isSubscribeRequest()) tag = "SUBSCRIBE_REQUEST";
		else if(request.isQueryRequest()) tag = "QUERY_REQUEST";
		else if(request.isUnsubscribeRequest()) tag = "UNSUBSCRIBE_REQUEST";
		else tag = "UNKNOWN_REQUEST";

		logTiming(tag,start,start);
	}

	public synchronized static void logTiming(Response response) {
		long start = getTime();

		String tag;
		if (response.isUpdateResponse()) tag = "UPDATE_RESPONSE";
		else if (response.isSubscribeResponse()) tag = "SUBSCRIBE_RESPONSE";
		else if(response.isQueryResponse()) tag = "QUERY_RESPONSE";
		else if(response.isUnsubscribeResponse()) tag = "UNSUBSCRIBE_RESPONSE";
		else if(response.isError()) tag = "ERROR_RESPONSE";
		else tag = "UNKNOWN_RESPONSE";

		logTiming(tag,start,start);
	}

	public static void debug(String s) {
		LogManager.getLogger().debug(s);
	}

	public static void debug(Response s) {
		LogManager.getLogger().debug(s);
	}

	public static boolean isTraceEnabled() {
		return LogManager.getLogger().isTraceEnabled();
	}

	public static void trace(String s) {
		LogManager.getLogger().trace(s);
	}

	public static void trace(Request s) {
		LogManager.getLogger().trace(s);
	}

	public static void trace(Response s) {
		LogManager.getLogger().trace(s);
	}

	public static void log(String customLevel,String message) {
		LogManager.getLogger().log(Level.getLevel(customLevel),message);
	}

	public static void log(String customLevel,String message,String context) {
		LogManager.getLogger().log(Level.getLevel(customLevel),message,context);
	}

	public static void warn(String s) {
		LogManager.getLogger().warn(s);
	}

	public static void warn(Response s) {
		LogManager.getLogger().warn(s.toString());
	}

	public static void info(String s) {
		LogManager.getLogger().info(s);
	}

	public static void error(String message) {
		LogManager.getLogger().error(message);
	}

	public static void error(Exception message) {
		LogManager.getLogger().error(message);
	}

	public static void error(Response message) {
		LogManager.getLogger().error(message.toString());
	}

	public static void fatal(String s) {
		LogManager.getLogger().fatal(s);
	}
}
