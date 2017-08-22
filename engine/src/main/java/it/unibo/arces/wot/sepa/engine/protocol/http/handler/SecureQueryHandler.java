package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class SecureQueryHandler extends QueryHandler {
	protected static final Logger logger = LogManager.getLogger("SecureQueryHandler");
	
	private AuthorizationManager am;
	
	public SecureQueryHandler(Scheduler scheduler, AuthorizationManager am,long timeout) throws IllegalArgumentException {
		super(scheduler, timeout);
		
		this.am = am;
	}
	
	@Override
	protected boolean authorize(HttpRequest request) {
		return am.authorizeRequest(request);
	}

}
