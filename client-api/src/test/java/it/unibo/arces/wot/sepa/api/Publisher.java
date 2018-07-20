package it.unibo.arces.wot.sepa.api;

import java.util.concurrent.atomic.AtomicLong;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Properties.HTTPMethod;
import it.unibo.arces.wot.sepa.commons.request.UpdateRequest;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class Publisher extends Thread  {
	private JSAP properties;
	private SEPASecurityManager sm;
	private SPARQL11Protocol client = null;
	private String id;
	
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
			try {
				client.update(buildUpdateRequest(id,5000));
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				
			}
			running.set(running.get()-1);
		}
	}
	
	public void interrupt() {
		super.interrupt();
		running.set(0);
	}
	
	protected UpdateRequest buildUpdateRequest(String id, int timeout) throws SEPAPropertiesException, SEPASecurityException {
		HTTPMethod method = properties.getUpdateMethod(id);
		String scheme = properties.getUpdateProtocolScheme(id);
		String host = properties.getUpdateHost(id);
		int port = properties.getUpdatePort(id);
		String path = properties.getUpdatePath(id);
		String sparql = properties.getSPARQLUpdate(id);
		String graphUri = properties.getUsingGraphURI(id);
		String namedGraphUri = properties.getUsingNamedGraphURI(id);

		String authorization = null;
		if (sm != null) authorization = sm.getAuthorizationHeader();
		
		return new UpdateRequest(method, scheme, host, port, path, sparql, graphUri, namedGraphUri,
				authorization,timeout);
	}
}
