package it.unibo.arces.wot.sepa.apps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class MeanCalculator extends Aggregator {
	private static final Logger logger = LogManager.getLogger("MeanCalculator");

	private float mean = 0;
	private long counter = 0;
	private String meanURI;

	private Bindings forcedBindings = new Bindings();

	private final String baseURI = "rnd:Mean-";

	public MeanCalculator() throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
			URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(new ApplicationProfile("randomNumbers.jsap"), "RANDOM_NUMBER", "UPDATE_MEAN");

		meanURI = baseURI + UUID.randomUUID();

		// Update!
		forcedBindings.addBinding("mean", new RDFTermURI(meanURI));
	}

	public boolean start() {
		Response ret;
		try {
			ret = subscribe(null);
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			logger.fatal(e.getMessage());
			return false;
		}
		if (ret.isError())
			return false;
		
		mean = 0;
		counter = 0;

		// Update!
		forcedBindings.addBinding("counter", new RDFTermLiteral(String.format("%d", counter)));
		forcedBindings.addBinding("value", new RDFTermLiteral(String.format("%.3f", mean)));
		update(forcedBindings);

		return true;
	}
	
	public void stop() {
		try {
			unsubscribe();
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		for (Bindings result : results.getBindings()) {
			logger.debug(result);
			if (result.getBindingValue("value") == null) {
				logger.warn("Value is null");
				continue;
			}
			float value;
			try {
				value = Float.parseFloat(result.getBindingValue("value").replace(",", "."));
			}
			catch(Exception e) {
				logger.error(e.getMessage());
				continue;
			}
			counter++;
			mean = ((mean * (counter - 1)) + value) / counter;
			logger.info(" mean: " + meanURI + " value: " + mean
					+ " counter: " + counter);
		}

		// Update!
		forcedBindings.addBinding("counter", new RDFTermLiteral(String.format("%d", counter)));
		forcedBindings.addBinding("value", new RDFTermLiteral(String.format("%.3f", mean)));
		update(forcedBindings);
	}
}
