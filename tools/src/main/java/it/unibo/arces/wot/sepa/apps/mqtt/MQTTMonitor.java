package it.unibo.arces.wot.sepa.apps.mqtt;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPAPropertiesException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPAProtocolException;
import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

public class MQTTMonitor {
	private static final Logger logger = LogManager.getLogger();

	// Produce observations coming from MQTT matching with the semantic mapping
	private static MQTTSmartifier smartifier;
	
	// Add observation based on the semantic mapping stored in JSAP
	private static MQTTMapper mqttInitializer;

	public static void main(String[] args)
			throws SEPAProtocolException, SEPASecurityException, SEPAPropertiesException, IOException, MqttException {
		if (args.length != 1) {
			logger.error("Please provide the jsap file as argument");
			System.exit(-1);
		}
		
		// Logger
		ObservationLogger analytics = new ObservationLogger(args[0]);
		analytics.subscribe(5000);
		
		// Remover
		ObservationRemover remover = new ObservationRemover(args[0]);
		remover.removeAll();
		remover.close();
		
		// Inizializer
		mqttInitializer = new MQTTMapper(args[0]);
		mqttInitializer.init();
		mqttInitializer.close();
		
		// Create MQTT smartifier
		smartifier = new MQTTSmartifier(args[0]);
		smartifier.start();
		
		logger.info("Press any key to exit...");
		try {
			System.in.read();
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		logger.info("Stop MQTT smartifier");
		smartifier.stop();

		logger.info("Stopped");
		
		analytics.close();
		
		System.exit(1);
	}
}
