package it.unibo.arces.wot.sepa.engine.processing;

public interface UpdateProcessorMBean {
	public void reset();
	
	public long getRequests();
	
	public float getTimingsCurrent();
	public float getTimingsMin();
	public float getTimingsAverage();
	public float getTimingsMax();
	
	public long getTimeout();
	public void setTimeout(long t);
	
	public void scale_ms();
	public void scale_us();
	public void scale_ns();
	public String getUnitScale();
}
