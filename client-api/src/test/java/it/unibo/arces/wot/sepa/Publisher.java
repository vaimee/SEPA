package it.unibo.arces.wot.sepa;

import static org.junit.jupiter.api.Assertions.assertFalse;

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

	private SPARQL11Protocol client;
	private final String id;

	private final AtomicLong running;

	private ConfigurationProvider provider;

	public Publisher(ConfigurationProvider provider,String id, long n) {

		this.id = id;

		running = new AtomicLong(n);
		
		this.setName("Publisher-" + id + "-" + this.getId());
		this.provider = provider;
				
		try {
			client = new SPARQL11Protocol(provider.getSecurityManager());
		} catch (SEPASecurityException e) {
			client = null;
			assertFalse(true,e.getMessage());
		}
	}

	public void run() {
		while (running.get() > 0) {
			try {
				Response ret = client.update(provider.buildUpdateRequest(id));
				
				if (ret.isError()) {
					ErrorResponse err = (ErrorResponse) ret;
					if (err.isTokenExpiredError()) {
						provider.getSecurityManager().refreshToken();
						ret = client.update(provider.buildUpdateRequest(id));
						assertFalse(ret.isError(),ret.toString());
					}
					//else assertFalse(true,err.toString());
				}
			} catch (SEPASecurityException | SEPAPropertiesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
