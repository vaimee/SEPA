package it.unibo.arces.wot.sepa.engine.protocol.ngsild;

public interface NgsiLdHandlerMBean {

	public void reset();

	public float getHandlingTime_ms();

	public float getHandlingMinTime_ms();

	public float getHandlingAvgTime_ms();

	public float getHandlingMaxTime_ms();
	
	public long getRequests();

	public long getErrors_Timeout();

	public long getErrors_CORSFailed();
	
	public long getErrors_ParsingFailed();
	
	public long getErrors_ValidatingFailed();
}
