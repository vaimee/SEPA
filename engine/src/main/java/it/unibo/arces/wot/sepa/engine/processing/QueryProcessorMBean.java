package it.unibo.arces.wot.sepa.engine.processing;

public interface QueryProcessorMBean {

	public void reset();
	
	public long getRequests();
	
	public float getTimingsCurrent();
	public float getTimingsMin();
	public float getTimingsAverage();
	public float getTimingsMax();
	
	public int getTimeout();
	public void setTimeout(int t);
	
	public void scale_ms();
	public void scale_us();
	public void scale_ns();
	public String getUnitScale();
}
