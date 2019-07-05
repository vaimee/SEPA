package it.unibo.arces.wot.sepa.apps.mqtt;

import java.util.ResourceBundle;

import org.eclipse.paho.client.mqttv3.logging.Logger;

public class Logging implements Logger {
	private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger();
	
	@Override
	public void initialise(ResourceBundle messageCatalog, String loggerID, String resourceName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResourceName(String logContext) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLoggable(int level) {
		return true;
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg) {
		logger.debug("SEVERE "+msg);	
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("SEVERE "+msg);	
	}

	@Override
	public void severe(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		logger.debug("SEVERE "+msg);
		
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg) {
		logger.debug("Warning "+msg);
		
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("Warning "+msg);
		
	}

	@Override
	public void warning(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		logger.debug("Warning "+msg);
		
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg) {
		logger.debug("info "+msg);
		
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("info "+msg);
		
	}

	@Override
	public void info(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		logger.debug("info "+msg);
		
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg) {
		logger.debug("Config "+msg);
		
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("Config "+msg);
		
	}

	@Override
	public void config(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable thrown) {
		logger.debug("Config "+msg);
		
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg) {
		logger.debug("[FINE]["+msg+"] "+sourceClass+"->"+sourceMethod);
		
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("[FINE]["+msg+"] "+sourceClass+"->"+sourceMethod);
		
	}

	@Override
	public void fine(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		logger.debug("[FINE]["+msg+"] "+sourceClass+"->"+sourceMethod);
		
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg) {
		logger.debug("finer "+msg);
		
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("finer "+msg);
		
	}

	@Override
	public void finer(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		logger.debug("finer "+msg);
		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg) {
		logger.debug("finest "+msg);
		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts) {
		logger.debug("finest "+msg);
		
	}

	@Override
	public void finest(String sourceClass, String sourceMethod, String msg, Object[] inserts, Throwable ex) {
		logger.debug("finest "+msg);
		
	}

	@Override
	public void log(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts,
			Throwable thrown) {
		logger.debug("log "+msg);
		
	}

	@Override
	public void trace(int level, String sourceClass, String sourceMethod, String msg, Object[] inserts,
			Throwable ex) {
		logger.debug("trace "+msg);
	}

	@Override
	public String formatMessage(String msg, Object[] inserts) {
		return msg;
	}

	@Override
	public void dumpTrace() {
		// TODO Auto-generated method stub
		
	}
}
