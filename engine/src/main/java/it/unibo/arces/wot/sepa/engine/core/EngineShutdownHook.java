package it.unibo.arces.wot.sepa.engine.core;

public class EngineShutdownHook extends Thread {
	private Engine engine;
	
	public EngineShutdownHook(Engine engine) {
		this.engine = engine;
	}
	
	public void run() {
		engine.shutdown();
	}
}
