package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

import static org.junit.Assert.*;

public class Publisher extends Thread  {
	protected final Logger logger = LogManager.getLogger();
	
	private final SEPASecurityManager sm;
	private final SPARQL11Protocol client;
	private final String id;
	
	private AtomicLong running;
	
	private static ConfigurationProvider provider;
	
	public Publisher(String id,SEPASecurityManager sm,long n) throws SEPAPropertiesException, SEPASecurityException {
		this.sm = sm;
		this.id = id;
		
		if (sm != null)
			client = new SPARQL11Protocol(sm);
		else
			client = new SPARQL11Protocol();
		
		running = new AtomicLong(n);
		
		provider = new ConfigurationProvider();
	}
	
	public void run() {
		while(running.get() > 0) {
			Response ret = client.update(provider.buildUpdateRequest(id,5000,sm));
			if (ret.isError()) {
				ErrorResponse error = (ErrorResponse) ret;
				logger.error(error);
				if (error.isTokenExpiredError()) {
					client.update(provider.buildUpdateRequest(id,5000,sm));
				}
				else
					assertFalse(error.toString(),true);
			}
			running.set(running.get()-1);
		}
	}
	
	public void finish() {
		running.set(0);
	}
}
