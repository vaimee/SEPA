package it.unibo.arces.wot.sepa.engine.processing.subscriptions;

public interface SubscribeProcessorMBean {
	public long getRequests();
	public long getSubscribeRequests();
	public long getUnsubscribeRequests();

	public long getSPUs_current();
	public long getSPUs_max();

	public float getSPUs_time();
	public float getSPUs_time_min();
	public float getSPUs_time_max();	
	public float getSPUs_time_average();

	public void reset();
	
	public void setKeepalive(int t);	
	public int getKeepalive();
	
	public long getSPUProcessingTimeout();
	public void setSPUProcessingTimeout(long t);
}
