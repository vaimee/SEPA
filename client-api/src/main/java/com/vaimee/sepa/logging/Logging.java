package com.vaimee.sepa.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Logging {
	private static Level defaultLevel = Level.DEBUG;

	public static Logger getLogger(){
		return LogManager.getLogger();
	}

	public static void setDefaultLevel(Level level) {
		defaultLevel = level;
	}
	public static Level getLevel(String level) {
		return (Level.getLevel(level) == null ? defaultLevel : Level.getLevel(level));
	}

	public static void init() {
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyyMMdd_HH_mm_ss"); // Quoted "Z" to indicate GMT, no timezone offset
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		System.setProperty("logFilename", nowAsISO);
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();

		System.out.println("");
		System.out.println("|------------ Logging ------------");
		
		final Configuration config = ctx.getConfiguration();
		LoggerConfig rootLoggerConfig = config.getLoggers().get("");
		Iterator<AppenderRef> it = rootLoggerConfig.getAppenderRefs().iterator();
		while (it.hasNext()) {
			AppenderRef ref = it.next();
			System.out.println("| Appender: <" + ref.getRef() + "> Level: " + ref.getLevel());
			if (rootLoggerConfig.getAppenderRefs().size() == 1 && ref.getRef().equals("OUT") && ref.getLevel() == null) {
				LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
				loggerConfig.setLevel(Level.ERROR);
				ctx.updateLoggers();
			}
		}
		
		System.out.println("| Logger level: " + getLogger().getLevel().toString());
		System.out.println("|---------------------------------");
	}
}
