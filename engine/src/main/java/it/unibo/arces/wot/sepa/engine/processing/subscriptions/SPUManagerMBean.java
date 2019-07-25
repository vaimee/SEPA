package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

public interface SPUManagerMBean {
	public long getUpdateRequests();
	public long getSubscribeRequests();
	public long getUnsubscribeRequests();

	public long getSPUs_current();
	public long getSPUs_max();
	public long getSubscribers();
	public long getSubscribers_max();
	
	public float getSPUs_time();
	public float getSPUs_time_min();
	public float getSPUs_time_max();	
	public float getSPUs_time_average();

	public float getFiltering_time();
	public float getFiltering_time_min();
	public float getFiltering_time_max();	
	public float getFiltering_time_average();
	
	public void reset();
	
	public long getSPUProcessingTimeout();
	public void setSPUProcessingTimeout(long t);
	
	public void scale_ms();
	public void scale_us();
	public void scale_ns();
	public String getUnitScale();
}
