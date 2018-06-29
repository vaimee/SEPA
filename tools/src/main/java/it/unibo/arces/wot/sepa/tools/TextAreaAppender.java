package it.unibo.arces.wot.sepa.tools;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JTextArea;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.OutputStreamAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;

class TextAreaAppender extends OutputStream {
	private JTextArea area;
	
	public TextAreaAppender(JTextArea area) {
		this.area = area;
	}
	
	@Override
	public void write(int b) throws IOException {
		try{
			area.append(String.format("%c", b));
			area.setCaretPosition(area.getDocument().getLength());
		}
		catch(Exception e) {
			
		}
	}	
	
	public static void addAppender(final OutputStream outputStream, final String outputStreamName) {
	    final LoggerContext context = LoggerContext.getContext(false);
	    final Configuration config = context.getConfiguration();
	    final PatternLayout layout = PatternLayout.newBuilder().withPattern("%d{ISO8601} [%-5level] %t (%F:%L) %msg%n%throwable").build();
	    final Appender appender = OutputStreamAppender.createAppender(layout, null, outputStream, outputStreamName, false, true);
	    appender.start();
	    config.addAppender(appender);
	    updateLoggers(appender, config);
	}

	private static void updateLoggers(final Appender appender, final Configuration config) {
	    final Level level = null;
	    final Filter filter = null;
	    for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
	        loggerConfig.addAppender(appender, level, filter);
	    }
	    config.getRootLogger().addAppender(appender, level, filter);
	}
}


