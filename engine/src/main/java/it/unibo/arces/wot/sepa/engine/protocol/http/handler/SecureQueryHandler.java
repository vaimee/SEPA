package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import org.apache.http.HttpRequest;

import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SecureQueryHandler extends QueryHandler implements SecureQueryHandlerMBean {
	
	private AuthorizationManager am;
	
	public SecureQueryHandler(Scheduler scheduler, AuthorizationManager am) throws IllegalArgumentException {
		super(scheduler);
		
		this.am = am;
	}
	
	@Override
	protected boolean authorize(HttpRequest request) {
		return am.authorizeRequest(request);
	}

	@Override
	public long getErrors_AuthorizingFailed() {
		return jmx.getErrors_AuthorizingFailed();
	}

}
