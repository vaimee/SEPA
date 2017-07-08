package it.unibo.arces.wot.sepa.tools;

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

import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public class GarbageCollector extends Aggregator {
	private int processedMessages = 0;
	
	private static final Logger logger = LogManager.getLogger("GarbageCollector");
	
	private static GarbageCollector chatServer;
	
	public GarbageCollector(ApplicationProfile appProfile, String subscribeID, String updateID) throws UnrecoverableKeyException, KeyManagementException, IllegalArgumentException, KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, URISyntaxException {
		super(appProfile,subscribeID, updateID);
	}

	@Override
	public void onAddedResults(BindingsResults bindingsResults) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			logger.info(processedMessages+ " "+bindings.toString());
			update(bindings);
		}
		
	}

	@Override
	public void onSubscribe(BindingsResults bindingsResults) {
		for (Bindings bindings : bindingsResults.getBindings()) {
			processedMessages++;
			logger.info( processedMessages+ " "+bindings.toString());
			update(bindings);
		}	
	}
	
	public static void main(String[] args) {
		
		ApplicationProfile profile = null;
		try {
			profile = new ApplicationProfile("GarbageCollector.jsap");
		} catch (NoSuchElementException | IOException | InvalidKeyException | IllegalArgumentException | NullPointerException | ClassCastException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		try {
			chatServer = new GarbageCollector(profile,"GARBAGE","REMOVE");
		} catch (UnrecoverableKeyException | KeyManagementException | IllegalArgumentException | KeyStoreException
				| NoSuchAlgorithmException | CertificateException | IOException | URISyntaxException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		try {
			if (chatServer.subscribe(null) == null) return;
		} catch (IOException | URISyntaxException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InterruptedException | UnrecoverableKeyException | KeyManagementException | KeyStoreException | CertificateException e1) {
			logger.fatal(e1.getLocalizedMessage());
			System.exit(1);
		}
		
		logger.info("Up and running");
		logger.info("Press any key to exit...");
		
		try {
			System.in.read();
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}


	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRemovedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribe() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onKeepAlive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBrokenSubscription() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSubscriptionError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub
		
	}

}
