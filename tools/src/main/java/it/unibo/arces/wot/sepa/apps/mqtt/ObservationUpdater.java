package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
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

public class ObservationUpdater extends Aggregator {
	private final Logger logger = LogManager.getLogger();

	private final TopicMapper mapper;

	public ObservationUpdater(JSAP jsap, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, SEPABindingsException {
		super(jsap, "MQTT_MESSAGES", "UPDATE_OBSERVATION_VALUE", sm);
		
		mapper = new TopicMapper(jsap, sm);
		mapper.subscribe(5000);	
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			// ?topic ?value ?broker
			String topic = bindings.getValue("topic");
			String value = bindings.getValue("value");

			if (value == null || topic == null) continue;
			if (value.equals("NaN")) continue;
			
			try {
				String[] observation = mapper.map(topic, value);

				if (observation == null) {
					logger.warn("Topic NOT found: " + topic);
					continue;
				}
				
				setUpdateBindingValue("observation", new RDFTermURI(observation[0]));
				setUpdateBindingValue("value", new RDFTermLiteral(observation[1],
						appProfile.getUpdateBindings("UPDATE_OBSERVATION_VALUE").getDatatype("value")));

				update();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
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
	public void onBrokenConnection() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorResponse errorResponse) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSubscribe(String spuid, String alias) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUnsubscribe(String spuid) {
		// TODO Auto-generated method stub

	}
}
