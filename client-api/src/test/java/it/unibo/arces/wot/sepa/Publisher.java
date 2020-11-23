package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.security.ClientSecurityManager;

public class Publisher extends Thread implements Closeable {
	protected final Logger logger = LogManager.getLogger();

	private SPARQL11Protocol client;
	private final String id;

	private final AtomicLong running;

	private ConfigurationProvider provider;
	private ClientSecurityManager sm;

	public Publisher(ConfigurationProvider provider, String id, long n)
			throws SEPASecurityException, SEPAPropertiesException {

		this.id = id;

		running = new AtomicLong(n);

		this.setName("Publisher-" + id + "-" + this.getId());
		this.provider = provider;
		this.sm = provider.buildSecurityManager();

		client = new SPARQL11Protocol(sm);
	}

	public void run() {
		while (running.get() > 0) {
			try {
				if (sm != null) sm.refreshToken();
				Response ret = client.update(provider.buildUpdateRequest(id, sm));
				if (ret.isError())
					logger.error(ret);
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				logger.error(e.getMessage());
			}
			
			running.set(running.get() - 1);
		}
	}

	public void close() throws IOException {
		try {
			client.close();
		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		if (sm != null) sm.close();

		running.set(0);
	}
}
