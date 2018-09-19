package it.unibo.arces.wot.sepa.engine.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EngineShutdownHook extends Thread {
	private static final Logger logger = LogManager.getLogger();
	
	private Engine engine;
	
	public EngineShutdownHook(Engine engine) {
		this.engine = engine;
		this.setName("SEPA Shutdown hook");
	}
	
	public void run() {
		try {
			engine.shutdown();
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}
}
