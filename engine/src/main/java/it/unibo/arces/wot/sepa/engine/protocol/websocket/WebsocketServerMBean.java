package it.unibo.arces.wot.sepa.engine.protocol.websocket;

public interface WebsocketServerMBean {
	public void reset();

	public long getMessages();
	
	public long getFragmented();
	
	public long getErrors();
}
