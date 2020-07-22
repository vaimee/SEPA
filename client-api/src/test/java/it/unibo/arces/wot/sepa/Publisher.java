package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;

public class Publisher extends Thread implements Closeable {
	protected final Logger logger = LogManager.getLogger();

	private final SPARQL11Protocol client;
	private final String id;

	private final AtomicLong running;

	private static ConfigurationProvider provider;

	public Publisher(String id, long n) throws SEPAPropertiesException, SEPASecurityException {
		this.setName("Publisher-" + id + "-" + this.getId());
		provider = new ConfigurationProvider();

		this.id = id;

		client = new SPARQL11Protocol(provider.getSecurityManager());

		running = new AtomicLong(n);
		
		if (provider.getJsap().isSecure()) provider.getSecurityManager().register("SEPATest");
	}

	public void run() {
		while (running.get() > 0) {
			client.update(provider.buildUpdateRequest(id));

			running.set(running.get() - 1);
		}
	}

	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
		
		running.set(0);
	}
}
