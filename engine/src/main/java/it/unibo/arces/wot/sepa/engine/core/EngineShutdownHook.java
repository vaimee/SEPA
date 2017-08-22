package it.unibo.arces.wot.sepa.engine.core;

public class EngineShutdownHook extends Thread {
	private Engine engine;
	
	public EngineShutdownHook(Engine engine) {
		this.engine = engine;
		this.setName("SEPA Shutdown hook");
	}
	
	public void run() {
		engine.shutdown();
	}
}
