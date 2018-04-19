package it.unibo.arces.wot.sepa.engine.protocol.http.handler;

import org.apache.http.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.engine.dependability.AuthorizationManager;
import it.unibo.arces.wot.sepa.engine.scheduling.Scheduler;

public class SecureUpdateHandler extends UpdateHandler implements SecureUpdateHandlerMBean {
	protected static final Logger logger = LogManager.getLogger("SecureUpdateHandler");
	
	private AuthorizationManager am;
	
	public SecureUpdateHandler(
			Scheduler scheduler, AuthorizationManager am) throws IllegalArgumentException {
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
