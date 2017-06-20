package it.unibo.arces.wot.sepa.engine.protocol.handler;

import org.apache.http.HttpRequest;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.protocol.HttpContext;

import it.unibo.arces.wot.sepa.engine.scheduling.SchedulerInterface;
import it.unibo.arces.wot.sepa.engine.security.AuthorizationManager;

public class SecureQueryHandler extends QueryHandler {
	private AuthorizationManager am;
	
	public SecureQueryHandler(HttpRequest request, HttpAsyncExchange exchange, HttpContext context,
			SchedulerInterface scheduler, AuthorizationManager am,long timeout) throws IllegalArgumentException {
		super(request, exchange, context, scheduler, timeout);
		
		this.am = am;
	}
	
	@Override
	protected boolean authorize(HttpRequest request) {
		return am.authorizeRequest(request);
	}

}
