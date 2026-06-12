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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Logging {

	public static class Timestamp {
		private final long timestamp;
		public Timestamp() {
			timestamp = System.nanoTime();
		}

		public long get() {
			return timestamp;
		}
	}

	private static Logger logger = LogManager.getLogger();

	private static String appendClassReference(String message) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
		String fullClassName = caller.getClassName(); // es. "com.vaimee.MyClass"
		String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
		return simpleClassName+".java:"+ caller.getLineNumber()+" "+message;
	}

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

	public synchronized static void logTiming(String tag, Timestamp start, Timestamp stop) {
		String message = String.format("%d,%d,%d,%d,%s",System.currentTimeMillis(),(stop.get()-start.get())/1000000,(stop.get()-start.get())/1000,stop.get()-start.get(),tag);
		logger.log(Level.getLevel("timing"),appendClassReference(message));
	}

	public synchronized static void logTiming(Request request) {
		String tag;
		if (request.isUpdateRequest()) tag = "UPDATE_REQUEST";
		else if (request.isSubscribeRequest()) tag = "SUBSCRIBE_REQUEST";
		else if(request.isQueryRequest()) tag = "QUERY_REQUEST";
		else if(request.isUnsubscribeRequest()) tag = "UNSUBSCRIBE_REQUEST";
		else tag = "UNKNOWN_REQUEST";

		logger.log(Level.getLevel("timing"),appendClassReference(String.format("%d,0,0,0,%s",System.currentTimeMillis(),tag)));
	}

	public static void logTiming(String s) {
		logger.log(Level.getLevel("timing"),appendClassReference(String.format("%d,0,0,0,%s",System.currentTimeMillis(),s)));
	}

	public synchronized static void logTiming(Response response) {
		String tag;
		if (response.isUpdateResponse()) tag = "UPDATE_RESPONSE";
		else if (response.isSubscribeResponse()) tag = "SUBSCRIBE_RESPONSE";
		else if(response.isQueryResponse()) tag = "QUERY_RESPONSE";
		else if(response.isUnsubscribeResponse()) tag = "UNSUBSCRIBE_RESPONSE";
		else if(response.isError()) tag = "ERROR_RESPONSE";
		else tag = "UNKNOWN_RESPONSE";

		logger.log(Level.getLevel("timing"),appendClassReference(String.format("%d,0,0,0,%s",System.currentTimeMillis(),tag)));
	}

	public static void debug(String s) {
		logger.debug(appendClassReference(s));
	}

	public static void debug(Response s) {
		logger.debug(appendClassReference(s.toString()));
	}

	public static boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	public static void trace(String s) {
		logger.trace(appendClassReference(s));
	}

	public static void trace(Request s) {
		logger.trace(appendClassReference(s.toString()));
	}

	public static void trace(Response s) {
		logger.trace(appendClassReference(s.toString()));
	}

	public static void log(String customLevel,String message) {
		logger.log(Level.getLevel(customLevel),appendClassReference(message));
	}

	public static void log(String customLevel,String message,String context) {
		logger.log(Level.getLevel(customLevel),appendClassReference(message),context);
	}

	public static void warn(String s) {
		logger.warn(appendClassReference(s));
	}

	public static void warn(Response s) {
		logger.warn(appendClassReference(s.toString()));
	}

	public static void info(String s) {
		logger.info(appendClassReference(s));
	}

	public static void error(String s) {
		logger.error(appendClassReference(s));
	}

	public static void error(Exception s) {
		logger.error(appendClassReference(s.toString()));
	}

	public static void error(Response s) {
		logger.error(appendClassReference(s.toString()));
	}

	public static void fatal(String s) {
		logger.fatal(appendClassReference(s));
	}
}
