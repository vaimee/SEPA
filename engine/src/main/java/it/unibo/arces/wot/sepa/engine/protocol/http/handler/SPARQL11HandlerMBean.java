package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

public interface SPARQL11HandlerMBean {
	public String getRequests();
	
	public String getCORSTimings();

	public String getParsingTimings();

	public String getValidatingTimings();

	public String getAuthorizingTimings();

	public String getHandlingTimings();
	
	public void setTimeout(long t);

	public long getTimeout();
	
	public void reset();
}
