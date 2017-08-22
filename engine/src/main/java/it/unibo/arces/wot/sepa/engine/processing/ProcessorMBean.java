package it.unibo.arces.wot.sepa.engine.processing;

public interface ProcessorMBean {
	public String getEndpointProperties();
	
	public String getRequests();
	
	public void resetQueryTimings();
	
	public void resetUpdateTimings();
	
	public String getQueryTimings();

	public String getUpdateTimings();
}
