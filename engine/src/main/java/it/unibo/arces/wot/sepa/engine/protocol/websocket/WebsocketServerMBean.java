package it.unibo.arces.wot.sepa.engine.protocol.websocket;

public interface WebsocketServerMBean {
	public void reset();

	public String getRequests();

	public void setKeepAlive(long period);

	public long getKeepAlive();
}
