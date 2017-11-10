package it.unibo.arces.wot.sepa.apps.randomnumbers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.response.Response;
import it.unibo.arces.wot.sepa.commons.response.SubscribeResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;

public class GarbageCollector extends Aggregator {
	private final Logger logger = LogManager.getLogger("GarbageCollector");
	private long numbers = 0;

	public GarbageCollector() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("randomNumbers.jsap"), "COUNT_NUMBERS", "DELETE_NUMBERS");
	}

	public boolean subscribe() {
		Response ret;

		ret = super.subscribe(null);

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
			logger.info("Total numbers: " + numbers + " GC numbers: "
					+ getApplicationProfile().getExtendedData().get("gcnumbers").getAsInt());
		}

		if (numbers >= getApplicationProfile().getExtendedData().get("gcnumbers").getAsInt()) {
			logger.info("Collecting triples...");
			update(null);
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
