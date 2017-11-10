package it.unibo.arces.wot.sepa.apps.randomnumbers;

import java.util.UUID;

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

public class MeanCalculator extends Aggregator {
	private static final Logger logger = LogManager.getLogger("MeanCalculator");

	private float mean = 0;
	private long counter = 0;
	private String meanURI;

	private Bindings forcedBindings = new Bindings();

	private final String baseURI = "rnd:Mean-";

	public MeanCalculator() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("randomNumbers.jsap"), "RANDOM_NUMBER", "UPDATE_MEAN");

		meanURI = baseURI + UUID.randomUUID();

		forcedBindings.addBinding("mean", new RDFTermURI(meanURI));
	}

	public boolean start() {
		Response ret;

		ret = subscribe(null);

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
		unsubscribe();

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
			} catch (Exception e) {
				logger.error(e.getMessage());
				continue;
			}
			counter++;
			mean = ((mean * (counter - 1)) + value) / counter;
			logger.info(" mean: " + meanURI + " value: " + mean + " counter: " + counter);
		}

		// Update!
		forcedBindings.addBinding("counter", new RDFTermLiteral(String.format("%d", counter)));
		forcedBindings.addBinding("value", new RDFTermLiteral(String.format("%.3f", mean)));
		update(forcedBindings);
	}

	@Override
	public void onResults(ARBindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPing() {
		// TODO Auto-generated method stub
		
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
