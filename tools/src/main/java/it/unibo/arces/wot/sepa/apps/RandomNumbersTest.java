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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class RandomNumbersTest {
	private static final Logger logger = LogManager.getLogger("RandomNumbers");

	// The application profile (JSAP)
	static ApplicationProfile myApp;

	// Aggregators
	static MeanCalculator meanCalculator;
	static GarbageCollector gc;

	// Consumers
	static MeanMonitor meanMonitor;

	public static void main(String[] args)
			throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException,
			InvalidKeyException, NoSuchElementException, NullPointerException, ClassCastException,
			NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		
		// First we create the application profile
		myApp = new ApplicationProfile("randomNumbers.jsap");

		// We can use the JSAP file also to store application specific
		// parameters (e.g., the number of random generator)
		int nGen = myApp.getExtendedData().get("generators").getAsInt();
		logger.info("Number of generators: " + nGen);

		// We start the consumers so we can monitor what is happening...
		meanMonitor = new MeanMonitor();			
		if (!meanMonitor.subscribe()) {
			logger.fatal("Failed to subscribe Mean Monitor");
			System.exit(1);
		}

		meanCalculator = new MeanCalculator();
		if (!meanCalculator.start()) {
			logger.fatal("Failed to subscribe Mean Calculator");
			System.exit(1);
		}
		
		gc = new GarbageCollector();
		if (!gc.subscribe()) {
			logger.fatal("Failed to subscribe Triples Counter");
			System.exit(1);
		}

		// We create and start the specified number of producers
		for (int i = 0; i < nGen; i++) {
			new RandomNumberGenerator();
		}

		System.out.println("Press any key to exit...");
		System.in.read();
	}
}
