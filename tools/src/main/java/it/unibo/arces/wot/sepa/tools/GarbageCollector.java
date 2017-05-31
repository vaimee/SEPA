package it.unibo.arces.wot.sepa.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;

public class GarbageCollector extends Aggregator {
	private int processedMessages = 0;
	
	private static final Logger logger = LogManager.getLogger("GarbageCollector");
	
	private static GarbageCollector chatServer;
	
	public GarbageCollector(ApplicationProfile appProfile, String subscribeID, String updateID) {
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
	
	public static void main(String[] args) throws FileNotFoundException, NoSuchElementException, IOException, URISyntaxException {
		
		ApplicationProfile profile = new ApplicationProfile("GarbageCollector.jsap");
		
		chatServer = new GarbageCollector(profile,"GARBAGE","REMOVE");
		
		if (chatServer.subscribe(null) == null) return;
		
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

}
