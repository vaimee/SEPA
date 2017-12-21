package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;
import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MQTTServerMonitoring {
	private static final Logger logger = LogManager.getLogger("MQTTServerMonitoring");

	// Produce observations coming from MQTT matching with the semantic mapping
	private static MQTTSmartifier adapter;
	
	// Add observation based on the semantic mapping stored in JSAP
	private static Producer mqttInitializer;

	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException {

		if (args.length != 1) {
			logger.error("Please specify the JSAP file (.jsap)");
			System.exit(1);
		}

		logger.info("Create initializer");

		mqttInitializer = new Producer(new ApplicationProfile(args[0]), "ADD_OBSERVATION");

		// Semantic mappings
		logger.info("Parse semantic mappings");
		JsonObject mappings = mqttInitializer.getApplicationProfile().getExtendedData().get("semantic-mappings")
				.getAsJsonObject();

		logger.info("Add observations");
		for (Entry<String, JsonElement> mapping : mappings.entrySet()) addObservation(mapping);

		// Create MQTT adapter
		logger.info("Create MQTT adapter");

		adapter = new MQTTSmartifier(args[0]);

		logger.info("Start MQTT adapter");

		if (adapter.start(mqttInitializer.getApplicationProfile().getExtendedData().get("simulate").getAsBoolean())) {
			logger.info("Press any key to exit...");
			try {
				System.in.read();
			} catch (IOException e) {
				logger.warn(e.getMessage());
			}

			logger.info("Stop MQTT adapter");
			adapter.stop();

			logger.info("Stopped");
		}
		
		System.exit(1);
	}

	private static void addObservation(Entry<String, JsonElement> mapping) {
		String topic = mapping.getKey();

		String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
		String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
		String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
		String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
		String label = mapping.getValue().getAsJsonObject().get("label").getAsString();
		
		Bindings bindings = new Bindings();
		bindings.addBinding("observation", new RDFTermURI(observation));
		bindings.addBinding("comment", new RDFTermLiteral(comment));
		bindings.addBinding("label", new RDFTermLiteral(label));
		bindings.addBinding("location", new RDFTermURI(location));
		bindings.addBinding("unit", new RDFTermURI(unit));
		bindings.addBinding("topic", new RDFTermLiteral(topic));

		logger.info("Add observation: " + bindings);
		mqttInitializer.update(bindings);
	}
}
