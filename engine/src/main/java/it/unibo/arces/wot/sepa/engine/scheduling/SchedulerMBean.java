package it.unibo.arces.wot.sepa.engine.scheduling;

public interface SchedulerMBean {
	public String getStatistics();

	public long getErrors();

	public long getQueue_Pending();

	public long getQueue_Max();

	public long getQueue_OutOfToken();

	public float getTimings_Update();

	public float getTimings_Query();

	public float getTimings_Subscribe();

	public float getTimings_Unsubscribe();

	public void reset();
}
