package it.unibo.arces.wot.sepa.engine.processing;

public interface ProcessorMBean {
	public String getEndpoint_Host();

	public String getEndpoint_Port();

	public String getEndpoint_QueryPath();

	public String getEndpoint_UpdatePath();

	public String getEndpoint_UpdateMethod();

	public String getEndpoint_QueryMethod();

	public void resetQueryTimings();

	public void resetUpdateTimings();

	public String getStatistics();

	public float getTimings_UpdateTime_ms();
	
	public float getTimings_QueryTime_ms();
}
