package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.ArrayList;

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
import it.unibo.arces.wot.sepa.pattern.DTNProducer;
import it.unibo.arces.wot.sepa.pattern.JSAP;
import it.unibo.dtn.JAL.exceptions.JALIPNParametersException;
import it.unibo.dtn.JAL.exceptions.JALLocalEIDException;
import it.unibo.dtn.JAL.exceptions.JALOpenException;
import it.unibo.dtn.JAL.exceptions.JALRegisterException;

public class MqttObservationUpdater extends Aggregator {
	private final Logger logger = LogManager.getLogger();

	private final MqttTopicMapper mapper;
	private DTNProducer DTNproducer = null;

	/**
	 * Creates a MqttObservationUpdater in HTTP mode
	 */
	public MqttObservationUpdater(JSAP jsap, SEPASecurityManager sm) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, SEPABindingsException {
		super(jsap, "MQTT_MESSAGES", "UPDATE_OBSERVATION_VALUE", sm);

		mapper = new MqttTopicMapper(jsap, sm);
		mapper.subscribe(5000);
	}
	
	/**
	 * Creates a MqttObservationUpdater in DTN or HTTP mode
	 */
	public MqttObservationUpdater(JSAP jsap, SEPASecurityManager sm, boolean enableDTN) throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, SEPABindingsException, JALLocalEIDException, JALOpenException, JALIPNParametersException, JALRegisterException {
		this(jsap, sm);

		if (enableDTN)
			this.DTNproducer = new DTNProducer(jsap, "UPDATE_OBSERVATION_VALUE", sm);
	}

	@Override
	public void onAddedResults(BindingsResults results) {
		for (Bindings bindings : results.getBindings()) {
			// ?topic ?value ?broker
			String topic = bindings.getValue("topic");
			String value = bindings.getValue("value");

			if (value == null || topic == null)
				continue;
			if (value.equals("NaN"))
				continue;

			try {
				ArrayList<String[]> observations = mapper.map(topic, value);

				if (observations == null) {
					logger.warn("Topic NOT found: " + topic);
					continue;
				}

				for (String[] observation : observations) {
					this.updateByObservation(observation);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}

	private void updateByObservation(String[] observation) throws SEPABindingsException, SEPASecurityException, IOException, SEPAPropertiesException {
		if (this.DTNproducer != null) { // DTN
			this.DTNproducer.setUpdateBindingValue("observation", new RDFTermURI(observation[0]));
			this.DTNproducer.setUpdateBindingValue("value", new RDFTermLiteral(observation[1],
					this.DTNproducer.getApplicationProfile().getUpdateBindings("UPDATE_OBSERVATION_VALUE").getDatatype("value")));
			
			this.DTNproducer.update();
		} else { // Not DTN
			this.setUpdateBindingValue("observation", new RDFTermURI(observation[0]));
			this.setUpdateBindingValue("value", new RDFTermLiteral(observation[1],
					this.DTNproducer.getApplicationProfile().getUpdateBindings("UPDATE_OBSERVATION_VALUE").getDatatype("value")));
			
			this.update();
		}
	}

	@Override
	public void onResults(ARBindingsResults results) {

	}

	@Override
	public void onRemovedResults(BindingsResults results) {

	}

	@Override
	public void onBrokenConnection() {

	}

	@Override
	public void onError(ErrorResponse errorResponse) {

	}

	@Override
	public void onSubscribe(String spuid, String alias) {

	}

	@Override
	public void onUnsubscribe(String spuid) {

	}
}
