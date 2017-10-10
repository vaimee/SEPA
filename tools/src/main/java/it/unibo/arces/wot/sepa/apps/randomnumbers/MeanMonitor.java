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
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;

public class MeanMonitor extends Consumer {
	private static final Logger logger = LogManager.getLogger("MeanMonitor");

	public MeanMonitor() throws IllegalArgumentException, UnrecoverableKeyException, KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
			URISyntaxException, InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		super(new ApplicationProfile("randomNumbers.jsap"), "MEAN");
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

		// Previous mean values
		for (Bindings binding : results.getBindingsResults().getBindings()) {
			logger.info(binding.getBindingValue("mean") + " : "
					+ Float.parseFloat(binding.getBindingValue("value").replaceAll(",", ".")) + " (values: "
					+ Integer.parseInt(binding.getBindingValue("counter")) + ")");
		}

		return true;
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings binding : results.getBindings()) {
			logger.info(binding.getBindingValue("mean") + " : "
					+ Float.parseFloat(binding.getBindingValue("value").replaceAll(",", ".")) + " (values: "
					+ Integer.parseInt(binding.getBindingValue("counter")) + ")");
		}

	}
}
