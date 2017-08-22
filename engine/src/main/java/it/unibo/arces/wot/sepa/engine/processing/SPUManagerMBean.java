package it.unibo.arces.wot.sepa.engine.processing;

public interface SPUManagerMBean {
	public String getActiveSPUs();

	public String getTimings();

	public void reset();
}
