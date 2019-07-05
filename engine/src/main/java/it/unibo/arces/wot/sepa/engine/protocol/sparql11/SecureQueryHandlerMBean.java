package it.unibo.arces.wot.sepa.engine.protocol.sparql11;

public interface SecureQueryHandlerMBean extends SPARQL11HandlerMBean {
	public long getErrors_AuthorizingFailed();
}
