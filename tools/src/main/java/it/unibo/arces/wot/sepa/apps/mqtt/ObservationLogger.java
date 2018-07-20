package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ObservationLogger extends Aggregator {

	public ObservationLogger(String jsap)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(new JSAP(jsap), "OBSERVATIONS", "LOG_QUANTITY");
	}

	@Override
	public void onResults(ARBindingsResults results) {}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings binding : results.getBindings()) {
			if (binding.getValue("value").equals("NaN")) continue;
			
			RDFTermLiteral literal = new RDFTermLiteral(binding.getValue("value"), binding.getDatatype("value"));
			
			this.setUpdateBindingValue("quantity", new RDFTermURI(binding.getValue("quantity")));
			this.setUpdateBindingValue("value", literal);
			this.setUpdateBindingValue("unit", new RDFTermURI(binding.getValue("unit")));
			
			try {
				update();
			} catch (SEPASecurityException | IOException | SEPAPropertiesException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onRemovedResults(BindingsResults results) {}

	@Override
	public void onBrokenConnection() {}

	@Override
	public void onError(ErrorResponse errorResponse) {}

	@Override
	public void onSubscribe(String spuid, String alias) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnsubscribe(String spuid) {
		// TODO Auto-generated method stub
		
	}
}
