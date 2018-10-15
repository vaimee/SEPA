package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.response.ErrorResponse;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.commons.sparql.ARBindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.BindingsResults;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.Aggregator;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class ObservationLogger extends Aggregator {
	private static final Logger logger = LogManager.getLogger();
	
	private static SEPASecurityManager sm = null;
	
	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, MqttException {
		if (args.length != 1) {
			logger.error("Please provide the jsap file as argument");
			System.exit(-1);
		}
		
		JSAP app = new JSAP(args[0]);
		if (app.isSecure()) sm = new SEPASecurityManager("sepa.jks", "sepa2017", "sepa2017",app.getAuthenticationProperties());
		
		// Logger
		ObservationLogger analytics = new ObservationLogger(app,sm);
		analytics.subscribe(5000);
		
		logger.info("Press any key to exit...");
		try {
			System.in.read();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
		
		analytics.close();
	}
	
	public ObservationLogger(JSAP jsap,SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {
		super(jsap, "OBSERVATIONS", "LOG_QUANTITY",sm);
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
