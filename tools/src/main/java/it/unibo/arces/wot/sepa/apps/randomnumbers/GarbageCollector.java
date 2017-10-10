package it.unibo.arces.wot.sepa.apps.randomnumbers;

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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class GarbageCollector extends Aggregator {
	private final Logger logger = LogManager.getLogger("GarbageCollector");
	private long numbers = 0;

	public GarbageCollector() throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
			URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(new ApplicationProfile("randomNumbers.jsap"), "COUNT_NUMBERS", "DELETE_NUMBERS");
	}

	public boolean subscribe() {
		Response ret;
		try {
			ret = super.subscribe(null);
		} catch (InvalidKeyException | UnrecoverableKeyException | KeyManagementException | NoSuchAlgorithmException
				| NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | KeyStoreException
				| CertificateException | IOException | URISyntaxException | InterruptedException e) {
			logger.fatal(e.getMessage());
			return false;
		}
		if (ret.isError())
			return false;

		SubscribeResponse results = (SubscribeResponse) ret;

		for (Bindings binding : results.getBindingsResults().getBindings()) {
			numbers += Integer.parseInt(binding.getBindingValue("numbers"));
			logger.info("Total numbers: " + numbers);
		}

		if (numbers >= getApplicationProfile().getExtendedData().get("gcnumbers").getAsInt()) {
			logger.info("Collecting triples...");
			update(null);
		}

		return true;
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		numbers = 0;
		for (Bindings binding : results.getBindings()) {
			numbers += Integer.parseInt(binding.getBindingValue("numbers"));
			logger.info("Total numbers: " + numbers+" GC numbers: "+getApplicationProfile().getExtendedData().get("gcnumbers").getAsInt());
		}

		if (numbers >= getApplicationProfile().getExtendedData().get("gcnumbers").getAsInt()) {
			logger.info("Collecting triples...");
			update(null);
		}

	}
}
