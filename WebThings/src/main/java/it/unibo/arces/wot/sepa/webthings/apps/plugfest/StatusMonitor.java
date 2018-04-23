package it.unibo.arces.wot.sepa.webthings.apps.plugfest;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class StatusMonitor extends Aggregator implements Runnable {
	protected static final Logger logger = LogManager.getLogger("StatusMonitor");

	private ConcurrentHashMap<String, Boolean> pings = new ConcurrentHashMap<String, Boolean>();
	private ConcurrentHashMap<String, Boolean> discoverables = new ConcurrentHashMap<String, Boolean>();

	
	public StatusMonitor() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("td.jsap"), "EVENT", "UPDATE_DISCOVER");

		Bindings bindings = new Bindings();
		bindings.addBinding("event", new RDFTermURI("wot:Ping"));
		subscribe(bindings);
	}

	@Override
	public void onResults(ARBindingsResults results) {
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			String thing = bindings.getBindingValue("thing");
			logger.info("Ping received by Web Thing: " + thing);

			synchronized (pings) {
				pings.put(thing, true);
			}
			
			if (discoverables.contains(thing))
				if (!discoverables.get(thing)) {
					logger.info("Make Web Thing: " + thing + " discoverable again");
					switchStatus(thing, true);
				}
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {

	}
	
	public static void main(String[] args) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		new Thread(new StatusMonitor()).start();
	}

	private void switchStatus(String thing, boolean status) {
		// Update
		Bindings bindings = new Bindings();
		if (status)
			bindings.addBinding("value", new RDFTermLiteral("true"));
		else
			bindings.addBinding("value", new RDFTermLiteral("false"));
		bindings.addBinding("thing", new RDFTermURI(thing));

		Response ret = update(bindings);

		if (ret.isUpdateResponse()) {
			discoverables.put(thing, status);

			if (status)
				logger.warn("Web Thing: " + thing + " turned ON");
			else
				logger.warn("Web Thing: " + thing + " turned OFF");
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(6000);
				logger.info("Check Web Things status...next check in 6 secs...");
			} catch (InterruptedException e) {

			}

			for (String thing : pings.keySet()) {
				if (!pings.get(thing))
					switchStatus(thing, false);
				pings.put(thing, false);
			}
		}
	}

	@Override
	public void onBrokenSocket() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub

	}

}
