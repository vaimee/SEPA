package it.unibo.arces.wot.sepa.engine.scheduling;

public interface SchedulerMBean {
	public String getRequests();

	public String getPendingRequests();

	public String getUpdateTimings();

	public String getQueryTimings();

	public String getSubscribeTimings();

	public String getUnsubscribeTimings();
	
	public void reset();
}
