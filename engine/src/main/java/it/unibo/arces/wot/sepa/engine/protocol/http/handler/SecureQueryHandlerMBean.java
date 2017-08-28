package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

public interface SecureQueryHandlerMBean extends SPARQL11HandlerMBean {
	public long getErrors_AuthorizingFailed();
}
