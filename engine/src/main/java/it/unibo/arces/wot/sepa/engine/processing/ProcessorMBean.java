package it.unibo.arces.wot.sepa.engine.processing;

public interface ProcessorMBean {
	public  String getEndpointHost();
	public  int getEndpointPort();
	public  String getEndpointQueryPath();
	public  String getEndpointUpdatePath();
	public  String getEndpointUpdateMethod();
	public  String getEndpointQueryMethod();
	public  int getMaxConcurrentRequests();
}
