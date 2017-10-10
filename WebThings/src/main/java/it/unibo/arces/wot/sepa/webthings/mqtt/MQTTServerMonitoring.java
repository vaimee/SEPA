package it.unibo.arces.wot.sepa.webthings.mqtt;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unibo.arces.wot.sepa.commons.sparql.Bindings;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermLiteral;
import it.unibo.arces.wot.sepa.commons.sparql.RDFTermURI;
import it.unibo.arces.wot.sepa.pattern.ApplicationProfile;
import it.unibo.arces.wot.sepa.pattern.Producer;

public class MQTTServerMonitoring {
	private static final Logger logger = LogManager.getLogger("MQTTSmartifier");
	
	private static MQTTSmartifier adapter;
	private static Producer mqttInitializer;
	
	public static void main(String[] args)  {

		if (args.length != 1) {
			logger.error("Please specify the JSAP file (.jsap)");
			System.exit(1);
		}
		
		logger.info("Create initializer");
		try {
			mqttInitializer = new Producer(new ApplicationProfile(args[0]),"ADD_OBSERVATION");
		} catch (UnrecoverableKeyException | KeyManagementException | InvalidKeyException | IllegalArgumentException
				| KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchElementException
				| NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | IOException | URISyntaxException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}

		// Semantic mappings
		logger.info("Parse semantic mappings");
		JsonObject mappings = mqttInitializer.getApplicationProfile().getExtendedData().get("semantic-mappings").getAsJsonObject();

		logger.info("Add observations");
		for (Entry<String, JsonElement> mapping : mappings.entrySet()) {
			String topic = mapping.getKey();

			String observation = mapping.getValue().getAsJsonObject().get("observation").getAsString();
			String unit = mapping.getValue().getAsJsonObject().get("unit").getAsString();
			String location = mapping.getValue().getAsJsonObject().get("location").getAsString();
			String comment = mapping.getValue().getAsJsonObject().get("comment").getAsString();
			String label = mapping.getValue().getAsJsonObject().get("label").getAsString();

			addObservation(observation, comment, label, location, unit, topic);
		}
		
		//Create MQTT adapter
		logger.info("Create MQTT adapter");
		try {
			adapter = new MQTTSmartifier(args[0]);
		} catch (UnrecoverableKeyException | KeyManagementException | InvalidKeyException | IllegalArgumentException
				| KeyStoreException | NoSuchAlgorithmException | CertificateException | NoSuchElementException
				| NullPointerException | ClassCastException | NoSuchPaddingException | IllegalBlockSizeException
				| BadPaddingException | IOException | URISyntaxException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		}

		logger.info("Start MQTT adapter");
		try {
			adapter.start();
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
				| CertificateException | MqttException | IOException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | URISyntaxException | InterruptedException e) {
			logger.fatal(e.getMessage());
			System.exit(1);
		} 

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
	
	private static void addObservation(String observation, String comment, String label, String location, String unit,
			String topic) {
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
