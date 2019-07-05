package it.unibo.arces.wot.sepa.apps.mqtt.mappers;

import java.util.ArrayList;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPABindingsException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.security.SEPASecurityManager;
import it.unibo.arces.wot.sepa.pattern.JSAP;

public class DefaultMapper extends MqttMapper {
	
	public DefaultMapper(JSAP appProfile, SEPASecurityManager sm)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, SEPABindingsException {
		super(appProfile, sm,null);
	}

	@Override
	protected ArrayList<String[]> map(String topic, String value) {
		ArrayList<String[]> ret = new ArrayList<String[]>();
	
		String observation = topic2observation.get(topic);
		
		if (observation != null) ret.add(new String[] { observation, value });
		else {
			logger.warn("Topic NOT found: "+topic+" value: "+value);
		}
		
		return ret;
	}
}
