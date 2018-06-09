package it.unibo.arces.wot.sepa.apps.mqtt;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MQTTInitializer extends Producer {
	private static final Logger logger = LogManager.getLogger();
	
	public MQTTInitializer() throws SEPAProtocolException, SEPAPropertiesException, SEPASecurityException {
		super(new ApplicationProfile("mqtt.jsap"), "ADD_OBSERVATION");
	}
	
	public void init() {
		logger.info("Parse semantic mappings");
		JsonObject mappings = getApplicationProfile().getExtendedData().get("semantic-mappings")
				.getAsJsonObject();

		logger.info("Add observations");
		for (Entry<String, JsonElement> mapping : mappings.entrySet()) addObservation(mapping);	
	}
	
	private void addObservation(Entry<String, JsonElement> mapping) {
		String topic = mapping.getKey();

		String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
		String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
		String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
		String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
		String label = mapping.getValue().getAsJsonObject().get("label").getAsString();

		setUpdateBindingValue("observation",observation);
		setUpdateBindingValue("comment",comment);
		setUpdateBindingValue("label",label);
		setUpdateBindingValue("location",location);
		setUpdateBindingValue("unit",unit);
		setUpdateBindingValue("topic",topic);
		update();
	}

}
