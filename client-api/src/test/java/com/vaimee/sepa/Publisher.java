package com.vaimee.sepa;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import com.vaimee.sepa.api.SPARQL11Protocol;
import com.vaimee.sepa.api.commons.exceptions.SEPAPropertiesException;
import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.api.commons.response.Response;
import com.vaimee.sepa.logging.Logging;

public class Publisher extends Thread implements Closeable {
	private SPARQL11Protocol client;
	private final String id;

	private final AtomicLong running;

	private ConfigurationProvider provider;

	public Publisher(ConfigurationProvider provider, String id, long n)
			throws SEPASecurityException, SEPAPropertiesException {

		this.id = id;

		running = new AtomicLong(n);

		this.setName("Publisher-" + id + "-" + this.threadId());
		this.provider = provider;

		client = new SPARQL11Protocol(provider.getClientSecurityManager());
	}

	public void run() {
		while (running.get() > 0) {
			try {
				//if (sm != null) sm.refreshToken();
				Response ret = client.update(provider.buildUpdateRequest(id));
				if (ret.isError())
					Logging.logger.error(ret);
			} catch (SEPAPropertiesException | SEPASecurityException e) {
				Logging.logger.error(e.getMessage());
			}
			
			running.set(running.get() - 1);
		}
	}

	public void close() throws IOException {
		try {
			client.close();
		} catch (IOException e) {
			Logging.logger.error(e.getMessage());
		}

//		if (sm != null) sm.close();

		running.set(0);
	}
}
