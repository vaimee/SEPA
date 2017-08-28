package it.unibo.arces.wot.sepa.engine.processing;

public interface SPUManagerMBean {
	public long getRequests();

	public long getSPUs_current();

	public long getSPUs_max();

	public float getSPUs_time();

	public String getSPUs_statistics();

	public void reset();
}
