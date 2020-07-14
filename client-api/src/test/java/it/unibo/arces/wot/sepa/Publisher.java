package it.unibo.arces.wot.sepa;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.protocol.SPARQL11Protocol;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;

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
	}

	public void run() {
		if (provider.getJsap().isSecure()) {
			try {
				provider.getSecurityManager().register("SEPATest");
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				logger.error(e);
			}
		}

		while (running.get() > 0) {
			Response ret = client.update(provider.buildUpdateRequest(id));

			int retryTimes = 0;
			while (ret.isError() && retryTimes < 10) {
				ErrorResponse errorResponse = (ErrorResponse) ret;

				if (errorResponse.isTokenExpiredError()) {
					try {
						provider.getSecurityManager().refreshToken();
					} catch (SEPAPropertiesException | SEPASecurityException e) {
						logger.error("Failed to refresh token: "+e.getMessage());
					}
				}
				else {
					logger.error(errorResponse);
				}
				
				ret = client.update(provider.buildUpdateRequest(id));
				retryTimes++;
			}

			running.set(running.get() - 1);
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
