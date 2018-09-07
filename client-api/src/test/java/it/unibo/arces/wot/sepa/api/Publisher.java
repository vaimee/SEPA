package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Publisher extends Thread  {
	protected final Logger logger = LogManager.getLogger();
	
	private final JSAP properties;
	private final SEPASecurityManager sm;
	private final SPARQL11Protocol client;
	private final String id;
	
	private AtomicLong running;
	
	public Publisher(String id,JSAP properties, SEPASecurityManager sm,long n) {
		this.properties = properties;
		this.sm = sm;
		this.id = id;
		
		if (sm != null)
			client = new SPARQL11Protocol(sm);
		else
			client = new SPARQL11Protocol();
		
		running = new AtomicLong(n);
	}
	
	public void run() {
		while(running.get() > 0) {
			Response ret = client.update(buildUpdateRequest(id,5000));
			if (ret.isError()) {
				ErrorResponse error = (ErrorResponse) ret;
				logger.error(error);
				if (error.isTokenExpiredError()) {
					client.update(buildUpdateRequest(id,5000));
				}
			}
			running.set(running.get()-1);
		}
	}
	
	public void finish() {
		running.set(0);
	}
	
	protected UpdateRequest buildUpdateRequest(String id, int timeout) {	
		String authorization = null;		
		if (sm != null)
			try {
				authorization = sm.getAuthorizationHeader();
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e.getMessage());
			}
		
		return new UpdateRequest(properties.getUpdateMethod(id), properties.getUpdateProtocolScheme(id), properties.getUpdateHost(id), properties.getUpdatePort(id), properties.getUpdatePath(id), properties.getSPARQLUpdate(id), properties.getUsingGraphURI(id), properties.getUsingNamedGraphURI(id),
				authorization,timeout);
	}
}
