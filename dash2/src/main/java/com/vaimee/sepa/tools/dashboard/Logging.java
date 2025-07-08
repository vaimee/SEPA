package com.vaimee.sepa.tools.dashboard;

import javax.swing.JTextArea;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Logging {
	private JTextArea log;
	
	private static final Logger logger = LogManager.getLogger();
	
	public Logging(JTextArea log) {
		this.log = log;
	}

	public void warn(String message) {
		log.append("[WARN] "+message+"\r\n");
		logger.warn(message);
		
	}

	public void error(String message) {
		log.append("[ERROR] "+message+"\r\n");
		logger.warn(message);
		
	}

	public void debug(String message) {
		log.append("[DEBUG] "+message+"\r\n");
		logger.warn(message);
		
	}

	public void info(String message) {
		log.append("[DEBUG] "+message+"\r\n");
		logger.warn(message);
	}
}
