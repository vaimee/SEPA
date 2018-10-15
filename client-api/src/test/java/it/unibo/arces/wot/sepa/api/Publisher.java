package it.unibo.arces.wot.sepa.api;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.ConfigurationProvider;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;

class Publisher extends Thread implements Closeable {
	protected final Logger logger = LogManager.getLogger();
	
	private final SEPASecurityManager sm;
	private final SPARQL11Protocol client;
	private final String id;
	
	private final AtomicLong running;
	
	private static ConfigurationProvider provider;
	
	public Publisher(String id,long n) throws SEPAPropertiesException, SEPASecurityException {
		provider = new ConfigurationProvider();
		
		this.id = id;
		
		if (provider.getJsap().isSecure()) {
			sm = provider.buildSecurityManager();
			client = new SPARQL11Protocol(sm);
		}
		else {
			sm = null;
			client = new SPARQL11Protocol();
		}
		
		running = new AtomicLong(n);
	}
	
	public void run() {
		while(running.get() > 0) {
			Response ret = client.update(provider.buildUpdateRequest(id,5000,sm));
			if (ret.isError()) {
				logger.error(ret);
			}
			running.set(running.get()-1);
		}
		
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}
	
	public void close() {
		running.set(0);
	}
}
