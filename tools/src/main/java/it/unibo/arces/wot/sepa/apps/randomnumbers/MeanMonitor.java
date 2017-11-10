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
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Consumer;

public class MeanMonitor extends Consumer {
	private static final Logger logger = LogManager.getLogger("MeanMonitor");

	public MeanMonitor() throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new ApplicationProfile("randomNumbers.jsap"), "MEAN");
	}

	public boolean subscribe() {
		Response ret;
		
		ret = super.subscribe(null);
		
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
