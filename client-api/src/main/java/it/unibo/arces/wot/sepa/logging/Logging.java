package it.unibo.arces.wot.sepa.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Logging {
	public static final Logger logger = LogManager.getLogger();
	
	public static org.apache.logging.log4j.Level getLevel(String level) {
		return (org.apache.logging.log4j.Level.getLevel(level) == null ? org.apache.logging.log4j.Level.OFF
				: org.apache.logging.log4j.Level.getLevel(level));
	}

	// Logging file name
	static {
		// Logging
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); // Quoted "Z" to indicate GMT, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		System.setProperty("logFilename", nowAsISO);
		org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager
				.getContext(false);
		ctx.reconfigure();
	}
	
	public static void printLog4jConfiguration() {
		System.out.println(">>> Logging <<<");
		System.out.println("Level: " + Logging.logger.getLevel().toString());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		LoggerConfig rootLoggerConfig = config.getLoggers().get("");
		Iterator<AppenderRef> it = rootLoggerConfig.getAppenderRefs().iterator();
		while (it.hasNext()) {
			AppenderRef ref = it.next();
			System.out.println("Appender: <" + ref.getRef() + "> Level: " + ref.getLevel());
		}
	}
}
